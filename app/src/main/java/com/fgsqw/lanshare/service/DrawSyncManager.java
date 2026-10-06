package com.fgsqw.lanshare.service;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.activity.DrawingActivity;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.io.IOException;
import java.net.Socket;

/**
 * 绘图同步管理器 - 管理设备间TCP长连接（双向同步）
 * <p>
 * 在"设备同步"模式下，APP通过TCP长连接与目标设备实时双向同步绘图数据。
 * 使用V4私有协议：
 * <pre>
 * 发起方: MAGIC → NEW_VERSION_4 → FS_DRAW_SYNC_REQUEST → 等待响应
 * 接收方: FileServer.handleDrawSync() → 弹出确认对话框 → 发送ACCEPT/REJECT
 * 双方:   进入双向读写循环（readLoop读远端事件 + sendDrawEvent写本地事件）
 * </pre>
 * </p>
 *
 * @author fgsq
 */
public class DrawSyncManager {

    /** 同步模式枚举 */
    public enum SyncMode {
        /** 网页同步 - 通过WebSocket与网页通讯 */
        WEB,
        /** 设备同步 - 通过TCP长连接与其他设备通讯 */
        DEVICE
    }

    private Socket socket;
    private CustomDataOutputStream outputStream;
    private CustomDataInputStream inputStream;
    private Device targetDevice;
    private volatile boolean connected = false;
    private ConnectionListener connectionListener;
    /** 断连回调（供接收端FileServer设置，断连时清理静态引用） */
    private Runnable onDisconnectCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 连接状态回调接口 */
    public interface ConnectionListener {
        void onConnected(Device device);
        void onConnectFailed(Device device, String error);
        void onDisconnected(Device device);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * 设置断连回调（供接收端FileServer调用，断连时清理DrawingActivity的静态引用）
     */
    public void setOnDisconnectCallback(Runnable callback) {
        this.onDisconnectCallback = callback;
    }

    /**
     * 连接到目标设备（发起方流程）
     * <p>流程：创建Socket → V4握手 → 发送FS_DRAW_SYNC_REQUEST → 等待ACCEPT → 进入双向循环</p>
     *
     * @param device 目标设备
     */
    public void connect(Device device) {
        disconnect();
        this.targetDevice = device;
        ThreadUtils.runThread(() -> {
            try {
                socket = LANService.getInstance().createSocket(device);
                outputStream = new CustomDataOutputStream(socket.getOutputStream());
                inputStream = new CustomDataInputStream(socket.getInputStream());

                // V4协议握手
                outputStream.writeInt(LCmd.NEW_VERSION_4);
                Device localDevice = LANService.getInstance().getDeviceManager().getDevice(device);
                outputStream.writeString(localDevice != null
                        ? localDevice.toJsonObject().toJSONString()
                        : "{}");
                outputStream.flush();

                // 发送绘图同步请求
                synchronized (this) {
                    outputStream.writeInt(LCmd.FS_DRAW_SYNC_REQUEST);
                    outputStream.writeString(LANService.getInstance().getDevName());
                    outputStream.flush();
                }

                // 等待对方响应（ACCEPT/REJECT）
                int response = inputStream.readInt();
                if (response != LCmd.FS_DRAW_SYNC_ACCEPT) {
                    // 对方拒绝
                    cleanup();
                    mainHandler.post(() -> {
                        if (connectionListener != null) {
                            connectionListener.onConnectFailed(device, "对方拒绝了绘图同步请求");
                        }
                    });
                    return;
                }

                // 对方接受，进入双向同步
                connected = true;
                mainHandler.post(() -> {
                    if (connectionListener != null) {
                        connectionListener.onConnected(device);
                    }
                });

                // 双向读取循环（阻塞直到连接断开）
                readLoop();

            } catch (Exception e) {
                cleanup();
                mainHandler.post(() -> {
                    if (connectionListener != null) {
                        connectionListener.onConnectFailed(device, e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * 发送绘图事件到目标设备（线程安全）
     *
     * @param drawJson 绘图事件JSON字符串
     */
    public void sendDrawEvent(String drawJson) {
        if (!connected || outputStream == null) return;
        ThreadUtils.runThread(() -> {
            writeCommand(LCmd.FS_DRAW, drawJson);
        });
    }

    /**
     * 发送绘图同步接受响应（线程安全，供接收端V4协议线程调用）
     * <p>必须在startBidirectionalLoop之前调用，确保ACCEPT写入与后续FS_DRAW写入使用同一把锁</p>
     *
     * @throws IOException 如果写入失败
     */
    public void sendAccept() throws IOException {
        synchronized (this) {
            outputStream.writeInt(LCmd.FS_DRAW_SYNC_ACCEPT);
            outputStream.flush();
        }
    }

    /**
     * 原子写入命令+数据到输出流（所有协议写入必须通过此方法，防止多线程写入交错）
     */
    private void writeCommand(int cmd, String data) {
        synchronized (this) {
            try {
                outputStream.writeInt(cmd);
                outputStream.writeString(data);
                outputStream.flush();
            } catch (IOException e) {
                handleDisconnect();
            }
        }
    }

    /**
     * 断开与目标设备的连接
     */
    public void disconnect() {
        Device device = targetDevice;
        cleanup();
        if (device != null && connectionListener != null) {
            mainHandler.post(() -> connectionListener.onDisconnected(device));
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Device getTargetDevice() {
        return targetDevice;
    }

    public void setTargetDevice(Device device) {
        this.targetDevice = device;
    }

    /**
     * 绑定Socket和流（供接收端FileServer调用，在sendAccept之前调用）
     */
    public void bindStreams(Socket socket_, CustomDataInputStream inputStream_, CustomDataOutputStream outputStream_) {
        this.socket = socket_;
        this.inputStream = inputStream_;
        this.outputStream = outputStream_;
        this.connected = true;
    }

    // ==================== 内部方法 ====================

    /**
     * 双向读取循环 - 持续读取远端发来的绘图命令
     * <p>此方法会阻塞当前线程，直到连接断开。
     * 同时支持：读取远端绘图事件 + 本地通过sendDrawEvent发送事件</p>
     */
    public void readLoop() {
        try {
            while (connected && socket != null && !socket.isClosed()) {
                int cmd = inputStream.readInt();
                if (cmd == LCmd.FS_DRAW) {
                    String drawJson = inputStream.readString();
                    handleIncomingDrawEvent(drawJson);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            if (connected) {
                handleDisconnect();
            }
        }
    }

    /**
     * 从接收端进入双向同步循环（供FileServer调用）
     * <p>接收端确认接受后调用此方法，进入与发起方相同的双向读写循环</p>
     *
     * @param socket_       已建立的Socket连接
     * @param inputStream_  输入流
     * @param outputStream_ 输出流
     */
    public void startBidirectionalLoop(Socket socket_, CustomDataInputStream inputStream_, CustomDataOutputStream outputStream_) {
        this.socket = socket_;
        this.inputStream = inputStream_;
        this.outputStream = outputStream_;
        this.connected = true;
        readLoop();
    }

    /**
     * 处理来自远端设备的绘图事件
     */
    private void handleIncomingDrawEvent(String drawJson) {
        try {
            JSONObject json = JSON.parseObject(drawJson);
            String action = json.getString("action");
            float nx = json.getFloatValue("x");
            float ny = json.getFloatValue("y");
            int color = json.getIntValue("color");
            float strokeWidth = json.getFloatValue("strokeWidth");
            DrawingActivity.handleRemoteDraw(action, nx, ny, color, strokeWidth);
        } catch (Exception e) {
            // 解析失败，忽略
        }
    }

    /**
     * 处理连接断开
     */
    private void handleDisconnect() {
        if (!connected) return;
        Device device = targetDevice;
        Runnable callback = onDisconnectCallback;
        cleanup();
        mainHandler.post(() -> {
            T.s("绘图设备连接已断开");
            if (connectionListener != null && device != null) {
                connectionListener.onDisconnected(device);
            }
            // 执行断连回调（清理接收端静态引用等）
            if (callback != null) {
                callback.run();
            }
        });
    }

    /**
     * 清理所有资源（不触发回调）
     */
    private void cleanup() {
        connected = false;
        IOUtil.closeIO(inputStream, outputStream, socket);
        inputStream = null;
        outputStream = null;
        socket = null;
    }
}
