package com.fgsqw.lanshare.service;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.*;
import androidx.annotation.Nullable;
import com.fgsqw.httpserver.utils.ByteUtil;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseService;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.db.FileSyncDBUtil;
import com.fgsqw.lanshare.db.MediaIdPathDBUtil;
import com.fgsqw.lanshare.db.MesssageDButil;
import com.fgsqw.lanshare.listener.ImageObserver;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;
import com.fgsqw.lanshare.receiver.NetWorkReceiver;
import com.fgsqw.lanshare.service.manager.DeviceManager;
import com.fgsqw.lanshare.service.manager.FileTransferManager;
import com.fgsqw.lanshare.service.manager.TcpServerManager;
import com.fgsqw.lanshare.service.manager.UdpServerManager;
import com.fgsqw.lanshare.service.version.four.FileSend;
import com.fgsqw.lanshare.service.version.four.FileServer;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.DateUtils;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.NotificationUtils;
import com.fgsqw.lanshare.utils.StringUtils;
import com.fgsqw.lanshare.web.LHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LAN服务 - 主服务类
 * 负责协调各个管理器完成局域网通信任务
 * @author fgsq
 * @version 1.0
 */
public class LANService extends BaseService {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(LANService.class);

    /** 日志标签 */
    public static final String TAG = "LANService";

    /** 服务实例 */
    public static LANService instance;

    /** 设备管理器 */
    private DeviceManager deviceManager;

    /** TCP 服务器管理器 */
    private TcpServerManager tcpServerManager;

    /** UDP 服务器管理器 */
    private UdpServerManager udpServerManager;

    /** 文件传输管理器 */
    private FileTransferManager fileTransferManager;

    /** 消息数据库工具 */
    private MesssageDButil messsageDButil;

    /** 媒体 ID 路径数据库工具 */
    private MediaIdPathDBUtil mediaIdPathDBUtil;

    /** 文件同步数据库工具 */
    private FileSyncDBUtil fileSyncDBUtil;

    /** HTTP 服务器 */
    private LHttpServer httpServer;

    /** 文件服务器 */
    private FileServer fileServer;

    /** 文件发送器 */
    private FileSend fileSend;

    /** 图片监听器 */
    private ImageObserver imageObserver;

    /** 网络监听器 */
    private NetWorkReceiver netWorkReceiver;

    /** Messenger,用于向 UI 发送消息 */
    private Messenger mMessenger;

    /** 系统音量 */
    private int systemVolume = 0;

    // ==================== 生命周期方法 ====================

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Object messenger = Objects.requireNonNull(intent.getExtras()).get("messenger");
            if (messenger != null) {
                mMessenger = (Messenger) messenger;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 初始化管理器
        initManagers();

        // 启动Web服务器
        startWebServer();

        // Service保活
        NotificationUtils.showBackendNotification(this, getString(R.string.app_name), getString(R.string.service_is_running));

        // 初始化数据
        initData();

        // 监听网络
        networkReceiver();

        // UDP广播监听
        udpServerManager.startServer();

        // 文件接收监听
        tcpServerManager.startServer();

        // 广播局域网所有设备我已上线
        deviceManager.notifyAllDevicesOnline();

        // UDP 广播扫描设备
        deviceManager.scanDevices();

        // 初始化媒体监听
        initMediaListener();
    }

    /**
     * 初始化管理器
     * <p>初始化数据库工具、文件服务和管理器</p>
     */
    private void initManagers() {
        // 初始化数据库工具
        messsageDButil = new MesssageDButil(this);
        mediaIdPathDBUtil = new MediaIdPathDBUtil(this);
        fileSyncDBUtil = new FileSyncDBUtil(this);

        // 初始化文件服务
        fileServer = new FileServer(this);
        fileSend = new FileSend(this);

        // 初始化管理器
        deviceManager = new DeviceManager(this, this);
        tcpServerManager = new TcpServerManager(this, deviceManager, fileServer);
        udpServerManager = new UdpServerManager(this, deviceManager);
        fileTransferManager = new FileTransferManager(this, deviceManager, fileSend, mediaIdPathDBUtil);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.debug("onDestroy 退出");

        // 停止管理器
        deviceManager.setRunning(false);
        tcpServerManager.stop();
        udpServerManager.stop();

        // 通知所有设备下线
        deviceManager.notifyAllDevicesOffline();

        // 注销媒体监听
        if (Config.MEDIA_SYNC && imageObserver != null) {
            imageObserver.unregisterObserver();
        }

        // 注销网络监听
        if (netWorkReceiver != null) {
            unregisterReceiver(netWorkReceiver);
        }
    }

    // ==================== 初始化方法 ====================

    /**
     * 启动 Web 服务器
     * <p>启动 HTTP 服务器,支持混合模式</p>
     */
    private void startWebServer() {
        httpServer = new LHttpServer(this);
        try {
            httpServer.startBlendingModeHttpServer();
        } catch (IOException e) {
            logger.error("error: ", e);
        }
    }

    /**
     * 注册网络监听器
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void networkReceiver() {
        netWorkReceiver = new NetWorkReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(NetWorkUtil.WIFI_AP_STATE_CHANGED_ACTION);
        registerReceiver(netWorkReceiver, filter);
    }

    /**
     * 初始化媒体监听
     * <p>注册媒体文件变化监听器</p>
     */
    private void initMediaListener() {
        imageObserver = new ImageObserver(this, new Handler());
        if (Config.MEDIA_SYNC) {
            imageObserver.registerObserver();
        }
    }

    // ==================== 公共接口方法 ====================

    /**
     * 获取服务实例
     *
     * @return 服务实例
     */
    public static LANService getInstance() {
        return instance;
    }

    /**
     * 发送UI交互消息
     */
    public void messageSend(Message message) {
        if (mMessenger == null) return;
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            logger.error("error: ", e);
        }
    }

    /**
     * 添加消息
     */
    public void addMessage(MessageContent messageContent) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
        mMessage.obj = messageContent;
        messageSend(mMessage);
        if (Config.SAVE_MESSAGE) {
            checkAndAddChatTime(messageContent.getId());
            messsageDButil.addMessage(messageContent);
        }
    }

    /**
     * 检查并添加聊天时间
     */
    public void checkAndAddChatTime(String bindId) {
        if (DateUtils.isFiveMinutesAgo(Config.lastMessageTime)) {
            com.fgsqw.lanshare.pojo.message.MessageTimeContent messageTimeContent =
                new com.fgsqw.lanshare.pojo.message.MessageTimeContent();
            messageTimeContent.setId(StringUtils.getUUID());
            messageTimeContent.setBindId(bindId);
            if (Config.SAVE_MESSAGE) {
                messsageDButil.addMessage(messageTimeContent);
            }
            Config.lastMessageTime = messageTimeContent.getCreateTime().getTime();
        }
    }

    /**
     * 发送显示进度消息
     */
    public void sendShowProgressMeg(List<MessageFileContent> messageFileContents) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_SHOW_PROGRESS;
        mMessage.obj = messageFileContents;
        messageSend(mMessage);
        if (Config.SAVE_MESSAGE) {
            checkAndAddChatTime(messageFileContents.get(0).getId());
            messsageDButil.addListMessage(messageFileContents);
        }
    }

    // ==================== 委托给管理器的方法 ====================

    /**
     * 添加设备
     */
    public void addDevice(Device device) {
        deviceManager.addDevice(device);
    }

    /**
     * 移除设备
     */
    public void removeDevice(String address) {
        deviceManager.removeDevice(address);
    }

    /**
     * 获取在线设备列表
     */
    public Map<String, Device> getOnLineDevices() {
        return deviceManager.getOnLineDevices();
    }

    /**
     * 获取设备列表
     */
    public List<Device> getDeviceList() {
        return deviceManager.getDeviceList();
    }

    /**
     * 获取本机设备信息
     */
    public List<Device> getSelfDevices() {
        return deviceManager.getSelfDevices();
    }

    /**
     * 获取设备名
     */
    public String getDevName() {
        return deviceManager.getDevName();
    }

    /**
     * 创建 Socket 连接
     *
     * @param device 目标设备
     * @return Socket 连接
     * @throws IOException 如果连接失败
     */
    public Socket createSocket(Device device) throws IOException {
        Socket socket = null;
        try {
            socket = makeSocket(device);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        if (socket == null || !socket.isConnected()) {
            T.s(String.format(getString(R.string.connection_to_device_failed), device.getDevName()));
            throw new IOException("Connection to device failed");
        }
        return socket;
    }

    /**
     * 创建 IPv6 设备
     *
     * @return IPv6 设备信息
     */
    public Device makeIPv6Device() {
        return deviceManager.makeIPv6Device();
    }

    /**
     * 初始化数据
     * <p>公开方法,委托给设备管理器</p>
     */
    public void initData() {
        deviceManager.initData();
    }

    /**
     * 创建 Socket
     *
     * @param device 目标设备
     * @return Socket 连接
     * @throws IOException 如果发生 I/O 错误
     */
    public Socket makeSocket(Device device) throws IOException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true); // 禁用Nagle算法，提高小包传输效率
        socket.setKeepAlive(true); // 保持连接
        socket.connect(new InetSocketAddress(device.getDevIP(), device.getDevPort()), 5000);
        CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
        outputStream.writeInt(Config.MAGIC_NUM);
        outputStream.flush();
        return socket;
    }

    /**
     * 创建 Socket
     *
     * @param host 目标地址
     * @param port 目标端口
     * @return Socket 连接
     * @throws IOException 如果发生 I/O 错误
     */
    public Socket makeSocket(InetAddress host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        OutputStream outputStream = socket.getOutputStream();
        byte[] magicBytes = ByteUtil.intToBytes(Config.MAGIC_NUM);
        outputStream.write(magicBytes);
        outputStream.flush();
        return socket;
    }

    /**
     * 开始接收文件
     */
    public void startReceivingFile(Device device, List<MessageFileContent> messageFileContents,
                              Socket client, InputStream input, OutputStream out,
                              boolean encData, boolean isAgree) {
        fileTransferManager.startReceivingFile(device, messageFileContents, client, input, out, encData, isAgree);
    }

    /**
     * 文件发送同步
     */
    public void fileSendSync(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        fileTransferManager.fileSendSync(fromDevice, device, fileList);
    }

    /**
     * 文件发送
     */
    public void fileSend(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        fileTransferManager.fileSend(fromDevice, device, fileList);
    }

    /**
     * 发送是否接收文件请求
     */
    public void sendIfReceiveFilesMsg(com.fgsqw.lanshare.service.RecvFileCallback recvFileCallback, int count) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
        mMessage.arg1 = count;
        mMessage.obj = recvFileCallback;
        messageSend(mMessage);
    }

    /**
     * 发送进度
     */
    public void sendProgressMeg(MessageFileContent fileContent) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_PROGRESS;
        mMessage.obj = fileContent;
        messageSend(mMessage);
    }

    /**
     * 关闭进度
     */
    public void sendCloseProgressMeg(MessageFileContent fileContent) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
        mMessage.obj = fileContent;
        messageSend(mMessage);
        if (Config.SAVE_MESSAGE) {
            messsageDButil.updateMessage(fileContent);
        }
    }

    /**
     * 发送消息
     * <p>广播消息到指定设备,默认不写入剪贴板</p>
     *
     * @param toDevice 目标设备
     * @param message  消息内容
     * @param isClip   是否写入剪贴板
     */
    public void broadcastMessage(Device toDevice, String message, boolean isClip) {
        broadcastMessage(toDevice, message, isClip, "", true);
    }

    /**
     * 发送消息
     * <p>广播消息到指定设备,支持指定包名</p>
     *
     * @param toDevice    目标设备
     * @param message     消息内容
     * @param isClip      是否写入剪贴板
     * @param packageName 包名
     */
    public void broadcastMessage(Device toDevice, String message, boolean isClip, String packageName) {
        broadcastMessage(toDevice, message, isClip, packageName, true);
    }

    /**
     * 发送消息
     *
     * @param toDevice    目标设备
     * @param msg         消息内容
     * @param isClip      是否写入剪贴板
     * @param packageName 包名
     * @param shareWS     是否分享到 WebSocket
     */
    public void broadcastMessage(Device toDevice, String msg, boolean isClip, String packageName, boolean shareWS) {
        udpServerManager.broadcastMessage(toDevice, msg, isClip, packageName, shareWS);
    }

    /**
     * 同步媒体
     * <p>同步媒体文件到局域网设备</p>
     */
    public void syncMedia() {
        fileTransferManager.syncMedia();
    }

    /**
     * 开始同步媒体文件
     *
     * @param device 目标设备
     * @param media  媒体列表
     */
    public void startSyncingMedias(Device device, List<MessageMediaContent> media) {
        fileTransferManager.startSyncingMedias(device, media);
    }

    /**
     * 更新应用
     * <p>从其他设备获取应用更新</p>
     *
     * @param device 目标设备
     */
    public void updateAppsFromOtherDevice(Device device) {
        fileTransferManager.updateAppsFromOtherDevice(device);
    }

    /**
     * 更新应用
     *
     * @param device     目标设备
     * @param jsonArray 应用信息数组
     */
    public void updateApp(Device device, com.alibaba.fastjson.JSONArray jsonArray) {
        fileTransferManager.updateApp(device, jsonArray);
    }

    // ==================== Getter方法 ====================

    /**
     * 获取 HTTP 服务器
     *
     * @return HTTP 服务器
     */
    public LHttpServer getHttpServer() {
        return httpServer;
    }

    /**
     * 获取文件发送器
     *
     * @return 文件发送器
     */
    public FileSend getFileSend() {
        return fileSend;
    }

    /**
     * 获取媒体 ID 路径数据库工具
     *
     * @return 媒体 ID 路径数据库工具
     */
    public MediaIdPathDBUtil getMediaIdPathDBUtil() {
        return mediaIdPathDBUtil;
    }

    /**
     * 获取消息数据库工具
     *
     * @return 消息数据库工具
     */
    public MesssageDButil getMesssageDButil() {
        return messsageDButil;
    }

    /**
     * 获取文件同步数据库工具
     *
     * @return 文件同步数据库工具
     */
    public FileSyncDBUtil getFileSyncDBUtil() {
        return fileSyncDBUtil;
    }

    /**
     * 获取设备管理器
     *
     * @return 设备管理器
     */
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    /**
     * 获取图片监听器
     *
     * @return 图片监听器
     */
    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    /**
     * 获取文件传输管理器
     *
     * @return 文件传输管理器
     */
    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }
}
