package com.fgsqw.lanshare.service.version;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DrawingActivity;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.DrawSyncManager;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.AESUtils;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * V4/V5 协议共用的扩展功能处理器
 * <p>消息接收、媒体同步、远程绘图同步等与协议版本无关的功能统一收敛于此，只实现一份，
 * 由 {@code four.FileServer} 与 {@code five.FileServerV5} 在各自命令分发后调用，
 * 版本协议链只保留真正存在版本差异的部分(文件传输)。</p>
 *
 * @author fgsq
 * @version 1.0
 */
public class SyncFeatureHandler {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(SyncFeatureHandler.class);

    /** 纯静态工具类，禁止实例化 */
    private SyncFeatureHandler() {
    }

    /**
     * 处理消息接收
     *
     * @param lanService     LAN 服务实例
     * @param device         发送方设备
     * @param socket         Socket 连接
     * @param inputStream    输入流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void handleMessage(LANService lanService, Device device, Socket socket,
                                     CustomDataInputStream inputStream) throws IOException {
        // 是否写入剪切板
        boolean isClip = inputStream.readBoolean();
        String messageEnc = inputStream.readString();
        logger.debug("tcp message: {}", messageEnc);
        try {
            String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
            MessageContent content = new MessageContent();
            content.setId(StringUtils.getUUID());
            content.setStatus(MessageContent.SUCCESS);
            content.setUserName(device.getDevName());
            content.setContent(message);
            content.setLeft(true);
            if (isClip) {
                // 写入剪贴板
                ClipboardManager cb = (ClipboardManager) lanService.getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("text", message));
                T.s((R.string.copy_text_to_clipboard_successful));
            }
            lanService.addMessage(content);
        } catch (Exception e) {
            logger.error("message decrypt error:", e);
            T.s((R.string.message_decryption_failed));
        } finally {
            IOUtil.closeIO(socket);
        }
    }

    /**
     * 处理媒体同步请求
     *
     * @param lanService     LAN 服务实例
     * @param device         发送方设备
     * @param socket         Socket 连接
     * @param inputStream    输入流
     * @param outputStream   输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void handleMediaSync(LANService lanService, Device device, Socket socket,
                                       CustomDataInputStream inputStream, CustomDataOutputStream outputStream) throws IOException {
        String mediaSyncJson = inputStream.readString();
        JSONArray mediaIdArray = JSON.parseArray(mediaSyncJson);
        JSONArray resultArray = new JSONArray();
        for (int i = 0; i < mediaIdArray.size(); i++) {
            long mediaId = mediaIdArray.getLong(i);
            if (!lanService.getMediaIdPathDBUtil().isIdExists(mediaId)) {
                resultArray.add(mediaId);
            }
        }
        outputStream.writeString(resultArray.toJSONString());
        outputStream.flush();
        IOUtil.closeIO(socket);
    }

    /**
     * 处理远程绘图同步（接收端流程）
     * <p>流程：读取同步请求 → 弹出确认对话框 → 接受则进入双向同步循环，拒绝则断开连接</p>
     *
     * @param lanService     LAN 服务实例
     * @param device         请求方设备
     * @param socket         Socket 连接
     * @param inputStream    输入流
     * @param outputStream   输出流
     */
    public static void handleDrawSync(LANService lanService, Device device, Socket socket,
                                      CustomDataInputStream inputStream, CustomDataOutputStream outputStream) {
        try {
            // 命令已由版本入口(handleVersion/handleVersion1)读取(FS_DRAW_SYNC_REQUEST)，这里直接读取请求方设备名
            String requesterName = inputStream.readString();

            // 在任意前台Activity弹出确认对话框，等待用户选择（阻塞60秒超时）
            boolean accepted = BaseActivity.showDrawSyncConfirm(requesterName, 60);

            if (!accepted) {
                // 拒绝：发送拒绝响应并断开
                outputStream.writeInt(LCmd.FS_DRAW_SYNC_REJECT);
                outputStream.flush();
                IOUtil.closeIO(socket);
                return;
            }

            // 接受：通过DrawSyncManager发送接受响应（与后续FS_DRAW写入共用同一把锁，防止写入交错）
            DrawSyncManager receiverSyncManager = new DrawSyncManager();
            receiverSyncManager.setTargetDevice(device);
            // 设置断连回调：断连时清理DrawingActivity的静态引用，防止重连时DrawingView找到已死的manager
            receiverSyncManager.setOnDisconnectCallback(DrawingActivity::clearReceiverSync);
            receiverSyncManager.bindStreams(socket, inputStream, outputStream);
            receiverSyncManager.sendAccept();

            // 判断当前是否已在绘图界面，避免重复跳转
            if (BaseActivity.getCurrentActivity() instanceof DrawingActivity) {
                // 已在绘图界面，直接设置接收端同步
                DrawingActivity.setupReceiverSync(receiverSyncManager, requesterName);
            } else {
                // 不在绘图界面，设置静态引用后启动DrawingActivity
                DrawingActivity.setReceiverSyncManager(receiverSyncManager);
                Intent drawIntent = new Intent(lanService, DrawingActivity.class);
                drawIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                drawIntent.putExtra("receiver_mode", true);
                drawIntent.putExtra("device_name", requesterName);
                lanService.startActivity(drawIntent);
            }

            // 进入双向同步循环（通过DrawSyncManager实现双向读写）
            ThreadUtils.runThread(() -> {
                try {
                    receiverSyncManager.readLoop();
                } catch (Exception e) {
                    logger.debug("Receiver draw sync ended: {}", e.getMessage());
                }
                // 不在这里closeIO，由协议线程finally统一关闭，避免双重关闭
            });

            // 等待双向同步线程结束（协议线程在此阻塞，保持连接不被上层关闭）
            // 由于startBidirectionalLoop内部会处理连接生命周期，这里直接返回即可
            // 注意：不能直接return，否则上层handleVersion会结束，但socket已在receiverSyncManager中管理
            // 使用简单等待方式：在双向循环结束前保持此线程
            while (receiverSyncManager.isConnected()) {
                Thread.sleep(500);
            }

        } catch (IOException e) {
            logger.debug("Draw sync connection closed: {}", device.getDevName());
        } catch (InterruptedException ignored) {
        } finally {
            IOUtil.closeIO(socket);
        }
    }
}
