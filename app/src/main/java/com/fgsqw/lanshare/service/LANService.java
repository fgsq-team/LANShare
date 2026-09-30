package com.fgsqw.lanshare.service;


import android.annotation.SuppressLint;
//import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.*;
//import android.telephony.PhoneStateListener;
//import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DeviceQrCodeActivity;
import com.fgsqw.lanshare.base.BaseService;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.db.FileSyncDBUtil;
import com.fgsqw.lanshare.db.MediaIdPathDBUtil;
import com.fgsqw.lanshare.db.MesssageDButil;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.listener.ImageObserver;
//import com.fgsqw.lanshare.listener.LFileObserver;
//import com.fgsqw.lanshare.listener.LPhoneStateListener;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.FileSyncData;
import com.fgsqw.lanshare.pojo.SendTask;
import com.fgsqw.lanshare.pojo.file.*;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.receiver.LANShareReceiver;
import com.fgsqw.lanshare.receiver.NetWorkReceiver;
import com.fgsqw.lanshare.service.version.four.FileSend;
import com.fgsqw.lanshare.service.version.four.FileServer;
import com.fgsqw.lanshare.service.version.four.FileTransfer;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;
import com.fgsqw.utils.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class LANService extends BaseService {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(LANService.class);

    public static final String TAG = "LANService";
    public static LANService instance;
    // TCP服务监听
    private ServerSocket fileReceive;
    // 在线设备列表
    private final Map<String, Device> onLineDevices = new ConcurrentHashMap<>();
    public final Map<String, Device> onLineWebDevices = new ConcurrentHashMap<>();
    private Messenger mMessenger;
    // 本机IP列表
    public Set<Device> localDevices = Collections.synchronizedSet(new HashSet<>());
    public List<NetInfo> ipv6NetInfoList = new ArrayList<>();
    private NetWorkReceiver netWorkReceiver;
    // 来电监听
//    private LPhoneStateListener phoneStateListener;
    private boolean running = true;
    // 系统音量保存
    int systemVolume = 0;
    // http服务
    private LHttpServer httpServer;
    public MediaIdPathDBUtil mediaIdPathDBUtil;
    private String currentIp = "";
    // 媒体变动监听
    private ImageObserver imageObserver;
    //    private LFileObserver lFileObserver;
    private FileSyncDBUtil fileSyncDBUtil;
    private FileServer fileServer;
    private FileSend fileSend;

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    private MesssageDButil messsageDButil;

    private void getIpv6() {
        ThreadUtils.runThread(() -> {
            Lock lock = StringLockManager.getStringLock("getIpv6");
            if (!lock.tryLock()) {
                return;
            }
            try {
                List<NetInfo> openIpv6 = NetWorkUtil.getOpenIpv6();
                ipv6NetInfoList.clear();
                ipv6NetInfoList.addAll(openIpv6);
            } finally {
                lock.unlock();
            }
        });
    }

    public void addDevice(Device device) {
        String address = device.getDevIP() + ":" + device.getDevPort();
        onLineDevices.put(address, device);
        LHttpServer.sendDeviceList();
    }

    public void removeDevice(String address) {
        onLineDevices.remove(address);
        LHttpServer.sendDeviceList();
    }

    public Map<String, Device> getOnLineDevices() {
        return onLineDevices;
    }

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
            currentIp = "";
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static LANService getInstance() {
        return instance;
    }

    private WifiManager.MulticastLock multicastLock;

    public void startWebServer() {
        httpServer = new LHttpServer(this);
        try {
            httpServer.startBlendingModeHttpServer();
        } catch (IOException e) {
            logger.error("error: ", e);
        }
    }

    @Override
    public void onCreate() {
        WifiManager mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = mWifiManager.createMulticastLock("multicastLock");
//        int batteryLevel = BatteryUtil.getBatteryLevel(this)[0];
//        logger.debug(debug("batteryLevel: " + batteryLevel);
//        multicastLock.setReferenceCounted(false);
        super.onCreate();
        fileServer = new FileServer(this);
        fileSend = new FileSend(this);
        messsageDButil = new MesssageDButil(this);
        mediaIdPathDBUtil = new MediaIdPathDBUtil(this);
        instance = this;
        startWebServer();
        // Service保活
        NotificationUtils.showBackendNotification(this, getString(R.string.app_name), getString(R.string.service_is_running));
        // 初始化数据
        initData();
        // 监听网络
        networkReceiver();
        // UDP广播监听
        udpServer();
        // 文件接收监听
        tcpServer();
        // 广播局域网所有设备我已上线
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, true);
            }
        });
        // UDP 广播扫描设备
        scanDevices();
        // 来电状态监听
//        callStateListen();
        initMediaListener();
//        if (Config.SYNC_NOTIFICATION) {
//            // 通知监听
//            initNotifiService();
//        }
    }

    private void initMediaListener() {
        fileSyncDBUtil = new FileSyncDBUtil(this);
        imageObserver = new ImageObserver(this, new Handler());
//        lFileObserver = new LFileObserver("/sdcard/LANShare", FileObserver.CREATE | FileObserver.DELETE | FileObserver.MODIFY | FileObserver.CLOSE_WRITE);
//        lFileObserver.startWatching();
        if (Config.MEDIA_SYNC) {
            imageObserver.registerObserver();
        }
    }

 /*   public void initNotifiService() {
        Intent intent = null;//启动服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(this, LNotifiService.class);
            startService(intent);//启动服务
        }
    }*/

    /**
     * 来电状态监听
     */
  /*  public void callStateListen() {
        phoneStateListener = new LPhoneStateListener();
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (SecurityException e) {
              logger.error("error: ",e);
        }

    }*/

    /**
     * @author fgsq
     * @comments 初始化设备IP地址列表
     * @date 2024/5/22 11:02
     */
    public void initData() {
        getIpv6();
    }

    /**
     * @author fgsq
     * @comments 网络状态监听，用来刷新设备IP
     * @date 2024/5/22 11:03
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void networkReceiver() {
        netWorkReceiver = new NetWorkReceiver(this);
        IntentFilter filter = new IntentFilter();
        // 监听网络状态
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // 监听AP状态
        filter.addAction(NetWorkUtil.WIFI_AP_STATE_CHANGED_ACTION);
        registerReceiver(netWorkReceiver, filter);
    }

    /**
     * @author fgsq
     * @comments 发送ui交互消息
     * @date 2024/5/22 11:03
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
     * @author fgsq
     * @comments cmd-新增设备
     * @date 2024/5/21 15:23
     */
    private void fsAddDevice(DataDec dataDec, Device device, Socket client, CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        boolean isIPV6 = dataDec.getBool();
//            if (device.isIPv4()) {
//                String hostAddress = client.getInetAddress().getHostAddress();
//                device.setDevIP(hostAddress);
//            } else {
//                String hostAddress = client.getInetAddress().getHostAddress();
//                device.setDevIP(hostAddress);
//            }
        device.setIPv4(!isIPV6);
        device.setCanRemove(false);
        addDevice(device);
        Device newDevice = null;
        if (isIPV6) {
            newDevice = makeIPv6Device();
        } else {
            for (Device localDevice : localDevices) {
                if (NetWorkUtil.subNet(localDevice.getDevIP(), device.getDevIP(), localDevice.getDevNetMask())) {
                    newDevice = localDevice;
                    break;
                }
            }
        }
        if (newDevice == null) {
            T.s((R.string.add_device_failed_devices_not_on_same_local_network));
            IOUtil.closeIO(out, input, client);
            return;
        }
        byte[] buffer = new byte[1024 * 1024];
        DataEnc dataEnc = makeDataEncCustomBuffer(newDevice, buffer, buffer.length);
        dataEnc.setCmd(LCmd.FS_ADD_DEVICE);
        dataEnc.putBool(isIPV6);
        IOUtil.write(out, dataEnc);
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
        }
        T.s(String.format(getString(R.string.add_device_successful), device.getDevName()));
        IOUtil.closeIO(input, out, client);
        DeviceQrCodeActivity.exitFlag = true;
    }

    /**
     * @author fgsq
     * @comments cmd-接收文件
     * @date 2024/5/21 15:23
     */
    private void fsShareFile(DataDec dataDec, Device device, Socket client, CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        byte[] buffer = new byte[1024 * 1024];
        // 接收文件
        // 文件数量
        int count = dataDec.getCount();
        boolean encData = dataDec.getBool();
        dataDec = new DataDec(buffer);
        // 数据包大小
        // 获取设备信息
        List<MessageFileContent> fileContentList = new ArrayList<>();
        Message mMessage;
        for (int i = 0; i < count; i++) {
            // 读取头数据
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                throw new RuntimeException("read error");
            // 从头数据中获取数据包大小
            int length = dataDec.getLength();
            // 接收数据包
            if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
                throw new RuntimeException("read error");
            dataDec.setData(buffer, buffer.length);
            // 文件大小
            long fileSize = dataDec.getLong();
            // 文件名称
            String fileName = dataDec.getString();
            int fileType = dataDec.getInt();
            String videoTime = dataDec.getString();
            logger.debug("filename:" + fileName + " fileSize:" + fileSize);
            if (fileType == LCmd.FILE_IMAGE || fileType == LCmd.FILE_VIEDO) {
                long mediaId = dataDec.getLongDefault(-1);
                MessageMediaContent mediaContent = new MessageMediaContent();
                mediaContent.setId(StringUtils.getUUID());
                mediaContent.setDataVersion(device.getDataVersion());
                mediaContent.setStatus(MessageContent.IN);
                mediaContent.setContent(fileName);
                mediaContent.setLength(fileSize);
                mediaContent.setIndex(i);
                mediaContent.setLeft(true);
                mediaContent.setUserName(device.getDevName());
                mediaContent.setVideo(fileType == LCmd.FILE_VIEDO);
                mediaContent.setVideoTime(videoTime);
                mediaContent.setMediaId(mediaId);
                mediaContent.setDevMode(device.getDevMode());
                fileContentList.add(mediaContent);
            } else if (fileType == LCmd.FILE_FOLDER) {
                // 获取文件数量
                int fileCount = dataDec.getInt();
                MessageFolderContent folderContent = new MessageFolderContent();
                folderContent.setId(StringUtils.getUUID());
                folderContent.setStatus(MessageContent.IN);
                folderContent.setContent(fileName);
                folderContent.setLength(fileSize);
                folderContent.setIndex(i);
                folderContent.setLeft(true);
                folderContent.setUserName(device.getDevName());
                folderContent.setFileCount(fileCount);
                folderContent.setDataVersion(device.getDataVersion());
                folderContent.setDevMode(device.getDevMode());
                fileContentList.add(folderContent);
            } else {
                MessageFileContent fileContent = new MessageFileContent();
                fileContent.setId(StringUtils.getUUID());
                fileContent.setStatus(MessageContent.IN);
                fileContent.setContent(fileName);
                fileContent.setLength(fileSize);
                fileContent.setIndex(i);
                fileContent.setLeft(true);
                fileContent.setUserName(device.getDevName());
                fileContent.setDataVersion(device.getDataVersion());
                fileContent.setDevMode(device.getDevMode());
                fileContentList.add(fileContent);
            }
        }

        FileTransfer fileTransfer = new FileTransfer();
        fileTransfer.setFiles(fileContentList);
        fileTransfer.setFromDevice(device);

        RecvFileCallback recvFileCallback = new RecvFileCallback(fileTransfer, fileContentList, client, input, out, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                startRecvFile(device, fileContentList, client, input, out, encData, isAgree);
            }
        };

        // 是否弹出确认接收dialog
        boolean isNotRecvDialog = App.getPrefUtil().getBoolean(PreConfig.NOT_RECV_DIALOG,true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            // 弹出是否接收文件请求弹窗
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
            mMessage.arg1 = count;
            mMessage.obj = recvFileCallback;
            messageSend(mMessage);
        }
    }

    /**
     * @author fgsq
     * @comments cmd-接收消息
     * @date 2024/5/21 15:24
     */
    private void fsMessage(DataDec dataDec, Device device, Socket client, CustomDataInputStream input, CustomDataOutputStream out) {
        try {
            String messageEnc = dataDec.getString();
            String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
            MessageContent content = new MessageContent();
            content.setId(StringUtils.getUUID());
            content.setStatus(MessageContent.SUCCESS);
            content.setUserName(device.getDevName());
            content.setContent(message);
            content.setLeft(true);
            addMessage(content);
        } catch (Exception e) {
            logger.error("message decrypt error:", e);
            T.s((R.string.message_decryption_failed));
        } finally {
            IOUtil.closeIO(out, input, client);
        }
    }

    /**
     * @author fgsq
     * @comments cmd-获取媒体同步
     * @date 2024/5/21 15:24
     */
    private void fsGetMediaSync(DataDec dataDec, Device device, Socket client, CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        int count = dataDec.getCount();
        byte[] buffer = new byte[1024 * 1024 * 2];
        DataEnc dataEnc = new DataEnc(buffer);
        dataEnc.setCount(count);
        int syncCount = 0;
        for (int i = 0; i < count; i++) {
            long mediaId = dataDec.getLong();
            if (!mediaIdPathDBUtil.isIdExists(mediaId)) {
                syncCount++;
                dataEnc.putLong(mediaId);
            }
        }
        dataEnc.setCount(syncCount);
        IOUtil.write(out, dataEnc);
    }

    /**
     * @author fgsq
     * @comments cmd-获取应用更新
     * @date 2024/5/21 15:24
     */
    private void fsUpdateApps(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        List<MessageApkContent> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            return;
        }
        byte[] buffer = new byte[1024 * 1024 * 2];
        dataDec = new DataDec(buffer);
        // 读取头数据
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        // 从头数据中获取数据包大小
        int length = dataDec.getLength();
        // 接收数据包
        if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
            throw new RuntimeException("read error");
        String apkArrayJson = dataDec.getString();
        JSONArray apkArray = JSON.parseArray(apkArrayJson);
        Iterator<Object> iterator = apkArray.iterator();
        JSONArray jsonArray = new JSONArray();
        while (iterator.hasNext()) {
            JSONObject apkObject = (JSONObject) iterator.next();
            String packageName = apkObject.getString("packageName");
            int versionCode = apkObject.getIntValue("versionCode");
            for (MessageApkContent apkInfo : apkFileList) {
                if (apkInfo.getPackageName().equals(packageName) && apkInfo.getVersionCode() > versionCode) {
                    apkObject.put("versionCode", apkInfo.getVersionCode());
                    jsonArray.add(apkObject);
                    iterator.remove();
                    break;
                }
            }
        }
        DataEnc dataEnc = new DataEnc(buffer);
        dataEnc.putString(jsonArray.toJSONString());
        IOUtil.write(out, dataEnc);
        IOUtil.closeIO(out, input, client);
    }

    /**
     * @author fgsq
     * @comments cmd-获取应用
     * @date 2024/5/21 15:24
     */
    private void fsGetApps(DataDec dataDec, Device device, Socket client, InputStream input, OutputStream out) throws Exception {
        List<MessageApkContent> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            T.s("APP列表为空");
            return;
        }
        byte[] buffer = new byte[1024 * 1024];
        dataDec = new DataDec(buffer);
        // 读取头数据
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        // 从头数据中获取数据包大小
        int length = dataDec.getLength();
        // 接收数据包
        if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
            throw new RuntimeException("read error");
        String apkArrayJson = dataDec.getString();
        JSONArray apkArray = JSON.parseArray(apkArrayJson);
        List<MessageFileContent> fileInfos = new ArrayList<>();
        Iterator<Object> iterator = apkArray.iterator();
        while (iterator.hasNext()) {
            JSONObject apkObject = (JSONObject) iterator.next();
            String packageName = apkObject.getString("packageName");
            for (MessageApkContent apkInfo : apkFileList) {
                if (apkInfo.getPackageName().equals(packageName)) {
                    fileInfos.add(apkInfo);
                    iterator.remove();
                    break;
                }
            }
        }
        IOUtil.closeIO(out, input, client);
        if (!fileInfos.isEmpty()) {
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_GET_APPS;
            mMessage.obj = new Object[]{device, fileInfos};
            messageSend(mMessage);
        }
    }


    /**
     * @author fgsq
     * @comments 处理tcp消息
     * @date 2024/5/21 15:40
     */
    public void handleTcp(Socket client, CustomDataInputStream input, CustomDataOutputStream out) {
        byte[] buffer = new byte[1024 * 1024];
        try {
            int cmd = input.readInt();
            DataDec dataDec = null;
            Device device;
            if (cmd == LCmd.NEW_VERSION_4) {
                String deviceJson = input.readString();
                device = new Device();
                device.fromJsonString(JSON.parseObject(deviceJson));
                if (device.getDataVersion() < LVersion.DATA_VERSION_4) {
                    device.setDataVersion(LVersion.DATA_VERSION_4);
                }
            } else {
                // 兼容旧版本
                new DataEnc(buffer).setCmd(cmd);
                // 兼容旧版本
                if (IOUtil.read(input, buffer, 4, DataEnc.getHeaderSize() - 4) != DataEnc.getHeaderSize() - 4)
                    return;
                // 自定义数据包解包工具
                dataDec = new DataDec(buffer, DataEnc.getHeaderSize());
                int length = dataDec.getLength();
                if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length) return;
                dataDec.setData(buffer, DataEnc.getHeaderSize() + length);
                // 设备端口
                int devPort = dataDec.getInt();
                // 设备ip
                String devIp = dataDec.getString();
                // 设备名
                String devName = dataDec.getString();
                // 设备类型
                int devMode = dataDec.getInt();
                // 设备唯一码
                String uniqueUUid = dataDec.getString();
                int dataVersion = dataDec.getInt();
                // 电量
                int batteryLevel = dataDec.getInt();
                byte chargeStatus = dataDec.getByte();
//            int webDeviceCount = dataDec.getInt();
                String address = devIp + ":" + devPort;
                device = onLineDevices.get(address);
                if (device == null) {
                    device = new Device();
                }
                device.setDevPort(devPort);
                device.setDevIP(devIp);
                device.setDevName(devName);
                device.setUniqueUUid(uniqueUUid);
                device.setDevMode(devMode);
                device.setSetTime(System.currentTimeMillis());
                device.setDataVersion(dataVersion);
                device.setBatteryLevel(batteryLevel);
                device.setChargeStatus(chargeStatus);
            }
            addDevice(device);
            // 协议处理
            handleVersion(cmd, dataDec, device, client, input, out);
        } catch (Exception e) {
            logger.error("handleTcp error", e);
        }
    }

    private void handleVersion(int cmd, DataDec dataDec, Device device, Socket client, CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        // 兼容旧版本
        if (cmd == LCmd.NEW_VERSION_4) {
            // 兼容新版本
            CustomDataInputStream inputStream = new CustomDataInputStream(input);
            CustomDataOutputStream outputStream = new CustomDataOutputStream(out);
            ThreadUtils.runThread(() -> {
                try {
                    fileServer.handleVersion1(device, client, inputStream, outputStream);
                } catch (Exception e) {
                    logger.error("handleVersion error", e);
                }
            });
        } else if (device.getDataVersion() < LVersion.DATA_VERSION_4) {
            if (cmd == LCmd.FS_ADD_DEVICE) {
                /* 添加设备 */
                fsAddDevice(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_SHARE_FILE) {
                /* 接收文件 */
                fsShareFile(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_MESSAGE) {
                /* 消息 */
                fsMessage(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_GET_NO_SYNC_MEDIA) {
                /* 获取没有同步的媒体列表 */
                fsGetMediaSync(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_UPDATE_APPS) {
                /* 更新APP */
                fsUpdateApps(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_GET_APPS) {
                /* 获取APP */
                fsGetApps(dataDec, device, client, input, out);
            }
        } else {
            T.s("不支持的版本");
        }
    }

    public void updateAppsFromOtherDevice(Device device) {
        ThreadUtils.runThread(() -> {
            try {
                Device fromDevice = getDevice(device);
                List<MessageApkContent> apkFileList = AnyData.apkFileList;
                if (apkFileList == null || apkFileList.isEmpty()) {
                    T.s("APP列表为空");
                    return;
                }
                JSONArray apkArray = new JSONArray();
                for (MessageApkContent apkInfo : apkFileList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("packageName", apkInfo.getPackageName());
                    jsonObject.put("versionCode", apkInfo.getVersionCode());
                    jsonObject.put("versionName", apkInfo.getVersionName());
                    apkArray.add(jsonObject);
                }
                byte[] bytes = apkArray.toJSONString().getBytes(FileUtil.UTF_8);
                Socket socket = makeSocket(device);
                OutputStream outputStream = socket.getOutputStream();
                DataEnc dataEnc = makeDataEnc(fromDevice, device, bytes.length + 100);
                dataEnc.setCmd(LCmd.FS_UPDATE_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                byte[] data = dataEnc.getData();
                InputStream inputStream = socket.getInputStream();
                DataDec dataDec = new DataDec(data);
                try {
                    // 读取头数据
                    if (IOUtil.read(inputStream, data, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                        return;
                } catch (IOException e) {
                    logger.error("error: ", e);
                    IOUtil.closeIO(inputStream, socket.getInputStream(), socket);
                    return;
                }
                // 从头数据中获取数据包大小
                int length = dataDec.getLength();
                try {
                    // 接收数据包
                    if (IOUtil.read(inputStream, data, DataEnc.getHeaderSize(), length) != length)
                        return;
                } catch (IOException e) {
                    logger.error("error: ", e);
                    IOUtil.closeIO(outputStream, inputStream, socket);
                    return;
                }
                IOUtil.closeIO(outputStream, inputStream, socket);
                String string = dataDec.getString();
                apkArray = JSON.parseArray(string);
                if (apkArray.isEmpty()) {
                    T.s("没有在" + device.getDevName() + "中找到更新");
                } else {
                    T.s("在" + device.getDevName() + "中找到" + apkArray.size() + "个更新");
                }
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_UPDATE_APPS;
                mMessage.obj = new Object[]{device, apkArray};
                messageSend(mMessage);
            } catch (IOException e) {
                logger.error("updateAppsFromOtherDevice:", e);
            }
        });
    }

    public void updateApp(Device device, JSONArray jsonArray) {
        ThreadUtils.runThread(() -> {
            byte[] bytes = jsonArray.toString().getBytes();
            Socket socket = null;
            try {
                socket = makeSocket(device);
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();
                Device fromDevice = getDevice(device);
                DataEnc dataEnc = makeDataEnc(fromDevice, device, bytes.length + 1024);
                dataEnc.setCmd(LCmd.FS_GET_APPS);
                IOUtil.write(outputStream, dataEnc);
                dataEnc.reset();
                dataEnc.putBytes(bytes);
                IOUtil.write(outputStream, dataEnc);
                IOUtil.closeIO(outputStream, inputStream, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // 接收文件缓存
    private final static byte[] recvBuffer = new byte[2 * 1024 * 1024];
    // 发送文件缓存
    private final static byte[] sendBuffer = new byte[2 * 1024 * 1024];

    public long baseRecv(
            Socket client, InputStream input, OutputStream out, DataDec dataDec,
            long fileLength, long mTotalRecv, long totalLength, File outFile,
            MessageFileContent fileContent
    ) {
        File parentFile = outFile.getParentFile();
        // 文件夹存在创建文件夹
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        // 文件输出流
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
//            outFileStream = new MappedByteBufferOutputStream(outFile.getPath(),fileLength,recvBuffer.length);
        } catch (IOException e) {
            logger.error("error: ", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            // 接收文件
            while (true) {
                // 接收文件信息
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {          // 数据
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                        break;
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        // 更新视图进度条
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        messageSend(mMessage);
                        p = progress;
                    }
                    if (fileContent.isTransfer()) {
                        IOUtil.write(out, LCmd.FS_NEXT);
                    } else {
                        thatTotal = -3;
                        IOUtil.write(out, LCmd.FS_BREAK);
                        // break;
                    }
                } else if (cmd == LCmd.FS_END) {    // 传输完毕
                    break;
                } else /*if (cmd == LCmd.FS_CLOSE)*/ {  // 被动关闭传输
                    logger.debug("close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }

    public long baseRecvDec(
            Socket client, InputStream input, OutputStream out, DataDec dataDec,
            long fileLength, long mTotalRecv, long totalLength, File outFile,
            MessageFileContent fileContent
    ) {
        File parentFile = outFile.getParentFile();
        // 文件夹存在创建文件夹
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            IOUtil.closeIO(input, out, client);
            return -1;
        }
        // 文件输出流
        OutputStream outFileStream;
        try {
            outFileStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            logger.error("error: ", e);
            T.s("打开文件：" + outFile.getPath() + "失败");
            return -1;
        }
        dataDec.reset();
        int p = 0;
        long totalRecv = mTotalRecv;
        long thatTotal = 0;
        try {
            // 接收文件
            while (true) {
                // 接收文件信息
                if (IOUtil.read(input, recvBuffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                    break;
                int cmd = dataDec.getByteCmd();
                if (cmd == LCmd.FS_DATA) {          // 数据
                    int thatLength = dataDec.getLength();
                    if (IOUtil.read(input, recvBuffer, DataEnc.getHeaderSize(), thatLength) != thatLength)
                        break;
                    mUtil.decData(recvBuffer, thatLength, DataEnc.getHeaderSize(), thatTotal);
                    IOUtil.write(outFileStream, recvBuffer, DataEnc.getHeaderSize(), thatLength);
                    totalRecv += thatLength;
                    thatTotal += thatLength;
                    int progress = (int) (totalRecv * 100 / totalLength);
                    if (progress != p) {
                        // 更新视图进度条
                        fileContent.setProgress(progress);
                        Message mMessage = Message.obtain();
                        mMessage.what = LCmd.SERVICE_PROGRESS;
                        mMessage.obj = fileContent;
                        messageSend(mMessage);
                        p = progress;
                    }
                    if (fileContent.isTransfer()) {
                        IOUtil.write(out, LCmd.FS_NEXT);
                    } else {
                        thatTotal = -3;
                        IOUtil.write(out, LCmd.FS_BREAK);
                        // break;
                    }
                } else if (cmd == LCmd.FS_END) {    // 传输完毕
                    break;
                } else /*if (cmd == LCmd.FS_CLOSE)*/ {  // 被动关闭传输
                    logger.debug("close");
                    T.s("接收：" + fileContent.getContent() + " 被中断");
                    thatTotal = -3;
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatTotal = 0;
        }
        IOUtil.closeIO(outFileStream);
        if (thatTotal != fileLength) {
            outFile.delete();
        }
        return thatTotal;
    }


    public void startRecvFile(Device device, List<MessageFileContent> messageFileContents, Socket client, InputStream input, OutputStream out, boolean encData, boolean isAgree) {
        ThreadUtils.runThread(() -> {
            Message mMessage;
            DataEnc dataEnc = new DataEnc();
            // 返回是否接收文件
            try {
                if (isAgree) {
                    dataEnc.setCmd(LCmd.FS_AGREE);
                    IOUtil.write(out, dataEnc);
                } else {
                    dataEnc.setCmd(LCmd.FS_NOT_AGREE);
                    IOUtil.write(out, dataEnc);
                    IOUtil.closeIO(input, out, client);
                    return;
                }
            } catch (IOException e) {
                IOUtil.closeIO(input, out, client);
                logger.error("error: ", e);
                return;
            }
            // 通知视图添加文件列表
            sendShowProgressMeg(messageFileContents);
//            try {
//                TimeUnit.MILLISECONDS.sleep(500);
//            } catch (InterruptedException ignored) {
//            }
            synchronized (recvBuffer) {
                // 接收文件列表遍历
                for (MessageFileContent fileContent : messageFileContents) {
                    // 接收文件总大小
                    long totalRecv = 0;
                    File file;
                    // 接收文件夹
                    if (fileContent instanceof MessageFolderContent) {
                        MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                        DataDec dataDec = new DataDec(recvBuffer);
                        file = new File(FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER), folderContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        for (int j = 0; j < folderContent.getFileCount(); j++) {
                            // 读取文件信息
                            try {
                                if (!IOUtil.read(input, dataDec)) break;
                            } catch (IOException e) {
                                logger.error("error: ", e);
                                break;
                            }
                            // 从头数据中获取数据包大小
                            long fileLength = dataDec.getLong();
                            String fileName = dataDec.getString();
                            logger.debug("接收文件:" + fileName + " 大小:" + fileLength);
                            File outFile = new File(FileUtil.classifyFile(Config.FILE_SAVE_PATH, Config.FOLDER), fileName);
                            // 防止重名文件覆盖
                            long thatTotal = 0;
                            if (encData) {
                                thatTotal = baseRecvDec(
                                        client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(),
                                        outFile, folderContent
                                );
                            } else {
                                thatTotal = baseRecv(
                                        client, input, out, dataDec,
                                        fileLength, totalRecv, folderContent.getLength(),
                                        outFile, folderContent
                                );
                            }
                            if (thatTotal == -3) {
                                break;
                            } else if (thatTotal <= 0) {
                                continue;
                            } else {
                                folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                                mMessage = Message.obtain();
                                mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                                mMessage.obj = folderContent;
                                messageSend(mMessage);
                            }
                            totalRecv += thatTotal;
                        }
                    } else {
                        // 接收单个文件
                        DataDec dataDec = new DataDec(recvBuffer);
                        String path = FileUtil.classifyFile(Config.FILE_SAVE_PATH, FileUtil.getNameType(fileContent.getContent()));
                        file = new File(path, fileContent.getContent());
                        file = FileUtil.avoidDuplication(file);
                        if (encData) {
                            totalRecv = baseRecvDec(
                                    client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(),
                                    file, fileContent
                            );
                        } else {
                            totalRecv = baseRecv(
                                    client, input, out, dataDec,
                                    fileContent.getLength(), 0, fileContent.getLength(),
                                    file, fileContent
                            );
                        }
                    }
                    // 接收成功设置文件路径 失败则删除文件
                    if (totalRecv != fileContent.getLength()) {
                        fileContent.setStatus(MessageContent.ERROR);
                        fileContent.setStateMessage("接收失败");
                    } else {
                        fileContent.setPath(file.getPath());
                        fileContent.setStatus(MessageContent.SUCCESS);
                        fileContent.setStateMessage("接收成功");
                        if (fileContent instanceof MessageMediaContent) {
                            Long mediaId = ((MessageMediaContent) fileContent).getMediaId();
                            if (mediaId > -1) {
                                mediaIdPathDBUtil.addMediaIdPath(mediaId, fileContent.getContent(), fileContent.getPath(), new Date(), true);
                            }
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        // 响应给发送方继续发送文件
                        IOUtil.write(out, 2);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    // 更新视图
                    mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                    mMessage.obj = fileContent;
                    messageSend(mMessage);
                    if (Config.SAVE_MESSAGE) {
                        messsageDButil.updateMessage(fileContent);
                    }
                    LHttpServer.sendMessage(fileContent.getContent(), fileContent.getUserName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), true, false);
                }
                IOUtil.closeIO(input, out, client);
            }
        });
    }

    public List<Device> getDeviceList() {
        List<Device> deviceList;
        if (!onLineDevices.isEmpty()) {
            deviceList = new ArrayList<>(onLineDevices.values());
        } else {
            deviceList = new ArrayList<>();
        }
        return deviceList;
    }

    public void syncMedia() {
        // 在这里处理新图片的逻辑
        List<FileSyncData> fileSyncData = fileSyncDBUtil.queryList();
        List<Device> deviceList = getDeviceList();
        List<Object[]> toDevice = new ArrayList<>();
        for (FileSyncData fileSyncDatum : fileSyncData) {
            for (Device device : deviceList) {
                if (fileSyncDatum.getDeviceId().equals(device.getUniqueUUid())) {
                    toDevice.add(new Object[]{device, fileSyncDatum});
                }
            }
        }
        if (!toDevice.isEmpty()) {
            for (PhotoFolder folder : AnyData.mediaResult.getmFolders()) {
                for (Object[] objects : toDevice) {
                    Device device = (Device) objects[0];
                    FileSyncData syncData = (FileSyncData) objects[1];
                    if (syncData.getFolderPath().equals(folder.getFolderPath())) {
                        List<MessageMediaContent> media = mUtil.deepCopyList(folder.getImages());
                        startSyncingMedias(device, media);
                    }
                }
            }
        }
           /* Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_MEDIA_CHANGES;
            mMessage.obj = null;
            lanService.messageSend(mMessage);*/
    }

    public void startSyncingMedias(Device device, List<MessageMediaContent> media) {
        ThreadUtils.runThread(() -> {
            if (device.getDataVersion() >= LVersion.DATA_VERSION_4) {
                fileSend.startSyncingMedias(device, media);
                return;
            }
            Lock lock = StringLockManager.getStringLock(device.getUniqueUUid());
            if (lock.tryLock()) {
                try {
                    Socket socket;
                    Device d;
                    if (device.isIPv4()) {
                        d = getDevice(device);
                    } else {
                        d = makeIPv6Device();
                    }
                    try {
                        socket = LANService.getInstance().makeSocket(device);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                        return;
                    }
                    DataEnc dataEnc = makeDataEnc(d, device, 1024 * 1024 * 4);
                    dataEnc.setCmd(LCmd.FS_GET_NO_SYNC_MEDIA);
                    dataEnc.setCount(media.size());
                    for (MessageMediaContent mediaInfo : media) {
                        dataEnc.putLong(mediaInfo.getMediaId());
//                        logger.debug( "MediaInfo: " + mediaInfo.getMediaId());
                    }
                    if (socket == null || !socket.isConnected()) {
                        T.s(String.format(getString(R.string.connection_to_device_failed), device.getDevName()));
                        return;
                    }
                    List<MessageFileContent> fileList = new ArrayList<>();
                    InputStream input = null;
                    OutputStream output = null;
                    try {
                        input = socket.getInputStream();
                        output = socket.getOutputStream();
                        TimeUnit.MILLISECONDS.sleep(10);
                        IOUtil.write(output, dataEnc);
                        DataDec dataDec = new DataDec(dataEnc.getData());
                        if (IOUtil.read(input, dataEnc.getData(), 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                            return;
                        int thatLength = dataDec.getLength();
                        if (IOUtil.read(input, dataEnc.getData(), DataEnc.getHeaderSize(), thatLength) != thatLength)
                            return;
                        int count = dataDec.getCount();
                        for (int i = 0; i < count; i++) {
                            long mediaId = dataDec.getLong();
                            MessageMediaContent mediaInfo = AnyData.mediaResult.getMediaInfoMap().get(mediaId);
                            if (mediaInfo != null) {
                                fileList.add(mediaInfo);
                            }
                            logger.debug("mediaId: " + mediaId);
                        }
                    } catch (Exception e) {
                        logger.error("error: ", e);
                    } finally {
                        IOUtil.closeIO(input, output, socket);
                    }
                    if (fileList.isEmpty()) {
                        return;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    fileSendSync(d, device, fileList);
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public void fileSend(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        ThreadUtils.runThread(() -> fileSendSync(fromDevice, device, fileList));
    }

    /**
     * 文件发送等待列表
     */
    List<SendTask> sendTasks = new Vector<>();

    /**
     * 创建Socket
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
     * 文件发送服务
     *
     * @param fileList 需要发送的文件列表
     */
    public void fileSendSync(Device fromDevice, Device device, List<MessageFileContent> fileList) {
        Socket socket = null;
        try {
            socket = createSocket(device);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            TimeUnit.MILLISECONDS.sleep(10);
            handleSend(fromDevice, device, socket, new CustomDataInputStream(input), new CustomDataOutputStream(output), fileList);
        } catch (Exception e) {
            logger.error("error: ", e);
            IOUtil.closeIO(input, output, socket);
        }
    }

    public void handleSend(Device fromDevice, Device toDevice, Socket socket, CustomDataInputStream input, CustomDataOutputStream output, List<MessageFileContent> fileList) throws IOException {
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        if (toDevice.getDataVersion() >= LVersion.DATA_VERSION_4) {
            fileSend.send(fromDevice, toDevice, socket, input, output, fileList);
        } else if (toDevice.getDataVersion() < LVersion.DATA_VERSION_4) {
            DataEnc dataEnc = makeDataEnc(fromDevice, toDevice, 1024 * 1024);
            dataEnc.setCmd(LCmd.FS_SHARE_FILE);
            dataEnc.setCount(fileList.size());
            dataEnc.putBool(encData);
            IOUtil.write(output, dataEnc);
            fileSend(fromDevice, toDevice, socket, input, output, fileList);
            ThreadUtils.runThread(() -> {
                Lock sendLock = StringLockManager.getStringLock("sendBuffer");
                try {
                    sendLock.lock();
                    for (SendTask sendTask : sendTasks) {
                        writeFiles(sendTask.getSocket(), sendTask.getFileTransfer().getFromDevice(), sendTask.getFileTransfer().getFiles(), sendTask.isEncData());
                    }
                    sendTasks.clear();
                } catch (Exception e) {
                    logger.error("error: ", e);
                } finally {
                    sendLock.unlock();
                }
            });
        }
    }

    /**
     * 发送文件
     */
    private void fileSend(Device fromDevice, Device toDevice, Socket socket, InputStream input, OutputStream out, List<MessageFileContent> fileList) {
        boolean encData = App.getPrefUtil().getBoolean(PreConfig.ENC_DATA);
        try {
            Device mDevice = null;
            if (toDevice.isIPv4()) {
                for (Device d : localDevices) {
                    if (!toDevice.isIPv4() || NetWorkUtil.subNet(d.getDevIP(), toDevice.getDevIP(), d.getDevNetMask())) {
                        mDevice = d;
                        break;
                    }
                }
            } else {
                mDevice = makeIPv6Device();
            }
            if (mDevice == null) {
                return;
            }
            DataEnc dataEnc = makeDataEnc(mDevice, toDevice, 1024 * 1024);
//            String userName = App.getPrefUtil().getString(PreConfig.USER_NAME);
            String userName = fromDevice.getDevName();
            for (int i = 0; i < fileList.size(); i++) {
                MessageFileContent fileInfo = fileList.get(i);
                dataEnc.reset();
                if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_STREAM) {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageStreamContent streamInfo = (MessageStreamContent) fileInfo;
                    streamInfo.setId(StringUtils.getUUID());
                    streamInfo.setContent(fileInfo.getName());
                    streamInfo.setLength(fileInfo.getLength());
                    streamInfo.setPath(fileInfo.getPath());
                    streamInfo.setIndex(i);
                    streamInfo.setLeft(false);
                    streamInfo.setUserName(userName);
                    streamInfo.setToUser(toDevice.getDevName());
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_IMAGE || fileInfo.getFileType() == MessageFileContent.FILE_TYPE_VIDEO) { // 媒体
                    MessageMediaContent mediaInfo = (MessageMediaContent) fileInfo;
                    dataEnc.putLong(fileInfo.getLength());
                    dataEnc.putString(fileInfo.getName());
                    if (mediaInfo.isVideo()) {
                        dataEnc.putInt(LCmd.FILE_VIEDO);
                        mediaInfo.setVideo(true);
                    } else {
                        dataEnc.putInt(LCmd.FILE_IMAGE);
                        mediaInfo.setVideo(false);
                    }
                    String videoTime = ((MessageMediaContent) fileInfo).getVideoTime();
                    dataEnc.putString(videoTime == null ? "" : videoTime);
                    if (mediaInfo.getMediaId() > -1) {
                        dataEnc.putLong(mediaInfo.getMediaId());
                    }
                    mediaInfo.setId(StringUtils.getUUID());
                    mediaInfo.setContent(fileInfo.getName());
                    mediaInfo.setLength(fileInfo.getLength());
                    mediaInfo.setPath(fileInfo.getPath());
                    mediaInfo.setIndex(i);
                    mediaInfo.setLeft(false);
                    mediaInfo.setUserName(userName);
                    mediaInfo.setToUser(toDevice.getDevName());
                    mediaInfo.setVideoTime(videoTime);
                    // 其他文件
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_URI) {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    MessageUriContent fileContent = (MessageUriContent) fileInfo;
                    fileContent.setId(StringUtils.getUUID());
                    fileContent.setContent(fileInfo.getName());
                    fileContent.setLength(fileInfo.getLength());
                    fileContent.setPath(fileInfo.getPath());
                    fileContent.setIndex(i);
                    fileContent.setLeft(false);
                    fileContent.setUserName(userName);
                    fileContent.setToUser(toDevice.getDevName());
                    // 文件夹
                } else if (fileInfo.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    File file = new File(fileInfo.getPath());
                    if (fileInfo instanceof MessageUriContent) {
                        MessageUriContent uriFileInfo = (MessageUriContent) fileInfo;
                        DocumentFile fileRealNameFromUri = FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri());
                        if (!fileRealNameFromUri.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    } else {
                        if (!file.exists()) {
                            T.s((R.string.path_not_found));
                            continue;
                        }
                    }
                    List<MessageFileContent> fileInfos = new LinkedList<>();
                    long totalSize = 0;
                    T.s((R.string.scanning_files));
                    // 区分Uri路径还是File路径
                    if (fileInfo instanceof MessageUriContent) {
                        MessageUriContent uriFileInfo = (MessageUriContent) fileInfo;
                        totalSize = FileSearchUtils.scanUriPathFileSize(FileUtil.getDocumentFileFromTreeUri(uriFileInfo.getUri()), new File(uriFileInfo.getPath()).getParent(), fileInfos);
                    } else {
                        // 扫描文件并返回扫描到的文件总大小
                        totalSize = FileSearchUtils.scanPathFileSize(file, fileInfos);
                        // 创建Message实体类
                        MessageFolderContent folderContent = (MessageFolderContent) fileInfo;
                        folderContent.setId(StringUtils.getUUID());
                        folderContent.setFileCount(fileInfos.size());
                        folderContent.setLength(totalSize);
                        folderContent.setChildren(fileInfos);
                        folderContent.setPath(file.getPath());
                        folderContent.setLeft(false);
                        folderContent.setContent(file.getName());
                        folderContent.setIndex(i);
                        folderContent.setUserName(userName);
                        folderContent.setToUser(toDevice.getDevName());
                        folderContent.setPath(file.getPath());
                    }
                    T.s((R.string.file_scan_complete));
                    // 写入总文件大小
                    dataEnc.putLong(totalSize);
                    // 写入文件名称
                    dataEnc.putString(file.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FOLDER);
                    dataEnc.putString("");
                    dataEnc.putInt(fileInfos.size());
                    // Uri分享文件
                } else {
                    // 写入文件大小
                    dataEnc.putLong(fileInfo.getLength());
                    // 写入文件名称
                    dataEnc.putString(fileInfo.getName());
                    // 写入文件类型
                    dataEnc.putInt(LCmd.FILE_FILE);
                    dataEnc.putString("");
                    fileInfo.setId(StringUtils.getUUID());
                    fileInfo.setIndex(i);
                    fileInfo.setLeft(false);
                    fileInfo.setUserName(userName);
                    fileInfo.setToUser(toDevice.getDevName());
                }
                IOUtil.write(out, dataEnc);
            }
            IOUtil.read(input, dataEnc.getBuffer(), 0, DataEnc.getHeaderSize());
            DataDec dataDec = new DataDec(dataEnc.getBuffer(), DataEnc.getHeaderSize());
            if (dataDec.getCmd() == LCmd.FS_NOT_AGREE) {
                T.s(toDevice.getDevName() + " " + getString(R.string.cancel_file_reception));
                IOUtil.closeIO(input, out, socket);
                return;
            }
            sendShowProgressMeg(fileList);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        Lock sendLock = StringLockManager.getStringLock("sendBuffer");
        boolean tryLock = sendLock.tryLock();
        if (tryLock) {
            try {
                writeFiles(socket, toDevice, fileList, encData);
            } catch (Exception e) {
                logger.error("error: ", e);
            } finally {
                sendLock.unlock();
            }
        } else {
            FileTransfer fileTransfer = new FileTransfer();
            fileTransfer.setFiles(fileList);
            fileTransfer.setFromDevice(fromDevice);
            sendTasks.add(new SendTask(socket, fileTransfer, encData));
        }
    }

    private void writeFiles(Socket socket, Device device, List<MessageFileContent> messageFileContents, boolean encData) {
        try {
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            DataEnc dataEnc = new DataEnc(sendBuffer);
            // 开始发送文件
            for (MessageFileContent fileContent : messageFileContents) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ignored) {
                }
                dataEnc.reset();
                // OutputStream 是用来判断文件取消的，下面写出文件必须要用它
                long totalSend = 0;
                if (fileContent.getFileType() == MessageFileContent.FILE_TYPE_FOLDER) {
                    MessageFolderContent folderContent = (MessageFolderContent) fileContent;
                    // 遍历需要传输的文件
                    for (MessageFileContent fileInfo : folderContent.getChildren()) {
                        File file = new File(folderContent.getPath());
                        String relativePath = fileInfo.getPath().replace(file.getParent(), "");
                        dataEnc.reset();
                        dataEnc.putLong(fileInfo.getLength());
                        dataEnc.putString(relativePath);
                        logger.debug("发送文件:" + relativePath + " 大小:" + fileInfo.getLength());
                        try {
                            IOUtil.write(out, dataEnc);
                        } catch (IOException e) {
                            logger.error("error: ", e);
                            break;
                        }
                        InputStream fileIs;
                        try {
                            // Uri文件转流
                            if (fileInfo instanceof MessageUriContent) {
                                fileIs = FileUtil.getInputStreamFromUri(((MessageUriContent) fileInfo).getUri());
                            } else {
                                fileIs = new FileInputStream(fileInfo.getPath());
                            }
                        } catch (FileNotFoundException e) {
                            logger.error("error: ", e);
                            continue;
                        }
                        // 文件发送
                        long thatSend = 0;
                        if (encData) {
                            // 文件发送
                            thatSend = baseSendEec(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        } else {
                            // 文件发送
                            thatSend = baseSend(fileContent, fileIs, input, out,
                                    dataEnc, totalSend, folderContent.getLength(), fileInfo.getLength());
                        }
                        if (thatSend == -3) {
                            break;
                        } else if (thatSend <= 0) {
                            continue;
                        } else {
                            folderContent.setCompleteCount(folderContent.getCompleteCount() + 1);
                            Message mMessage = Message.obtain();
                            mMessage.what = LCmd.SERVICE_COMPLETE_COUNT;
                            mMessage.obj = folderContent;
                            messageSend(mMessage);
                        }
                        totalSend += thatSend;
                    }
                } else {
                    InputStream fileIs;
                    try {
                        if (fileContent instanceof MessageStreamContent) {
                            fileIs = ((MessageStreamContent) fileContent).getInputStream();
                        } else if (fileContent instanceof MessageUriContent) {
                            fileIs = FileUtil.getInputStreamFromUri(((MessageUriContent) fileContent).getUri());
                        } else {
                            fileIs = new FileInputStream(fileContent.getPath());
//                            fileIs = new MappedByteBufferInputStream(fileContent.getPath(), sendBuffer.length);
                        }
                    } catch (FileNotFoundException e) {
                        logger.error("error: ", e);
                        T.s("发送文件失败，文件：" + fileContent.getContent() + "不存在");
                        continue;
                    }
                    if (encData) {
                        // 文件接收
                        totalSend += baseSendEec(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    } else {
                        // 文件接收
                        totalSend += baseSend(fileContent, fileIs, input, out, dataEnc, 0, fileContent.getLength(), fileContent.getLength());
                    }
                }
                try {
                    // 等待接收方响应再继续发送文件
                    int read = input.read();
                } catch (IOException e) {
                    logger.error("error: ", e);
                }
                if (totalSend != fileContent.getLength()) {
                    fileContent.setStatus(MessageContent.ERROR);
                    fileContent.setStateMessage("发送失败");
                } else {
                    fileContent.setStatus(MessageContent.SUCCESS);
                    fileContent.setStateMessage("发送成功");
                }
                LHttpServer.sendMessage(fileContent.getContent(), fileContent.getToUser() + " ← " + getDevName(), fileContent.getPath(), fileContent instanceof MessageMediaContent ? 1 : 2, 0, FileUtil.computeSize(fileContent.getLength()), false, false);
                Message mMessage = Message.obtain();
                mMessage.what = LCmd.SERVICE_CLOSE_PROGRESS;
                mMessage.obj = fileContent;
                messageSend(mMessage);
                if (Config.SAVE_MESSAGE) {
                    messsageDButil.updateMessage(fileContent);
                }
                logger.debug("发送成功:" + totalSend);
            }
        } catch (Exception e) {
            logger.error("error: ", e);
        } finally {
            IOUtil.closeIO(socket);
        }
    }

    public long baseSend(
            MessageFileContent content, InputStream fileIs,
            InputStream mInput, OutputStream mOut, DataEnc dataEnc,
            long mTotalSend, long totalLength, long fileLength
    ) {
        // 发送文件
//        logger.debug( "发送文件:" + filePath + " 大小:" + fileLength);
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        int read = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            // 读取时偏移掉头的位置
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                IOUtil.write(mOut, dataEnc);
                read = mInput.read();
                if (read == LCmd.FS_BREAK) {
                    thatSend = 0;
                    T.s("发送文件：" + content.getContent() + " 被中断");
                    break;
                }
                if (!content.isTransfer()) {
                    thatSend = -3;
                    break;
                }
                totalSend += ten;
                thatSend += ten;
                int progress = (int) (totalSend * 100 / totalLength);
                if (progress != p) {
                    content.setProgress(progress);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_PROGRESS;
                    mMessage.obj = content;
                    messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatSend = 0;
        }
        dataEnc.reset();
        if (thatSend != fileLength) {
            dataEnc.setByteCmd(LCmd.FS_CLOSE);
        } else {
            dataEnc.setByteCmd(LCmd.FS_END);
        }
        try {
            IOUtil.write(mOut, dataEnc);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        return thatSend;
    }

    public long baseSendEec(
            MessageFileContent content, InputStream fileIs,
            InputStream mInput, OutputStream mOut, DataEnc dataEnc,
            long mTotalSend, long totalLength, long fileLength
    ) {
        // 发送文件
//        logger.debug( "发送文件:" + filePath + " 大小:" + fileLength);
        int ten;
        int p = 0;
        dataEnc.reset();
        dataEnc.setByteCmd(LCmd.FS_DATA);
        long totalSend = mTotalSend;
        long thatSend = 0;
        int read = 0;
        try {
            int dataLength = sendBuffer.length - DataEnc.getHeaderSize();
            content.setStatus(MessageContent.IN);
            // 读取时偏移掉头的位置
            while ((ten = fileIs.read(sendBuffer, DataEnc.getHeaderSize(), dataLength)) != -1) {
                dataEnc.setDataIndex(ten);
                mUtil.encData(sendBuffer, ten, DataEnc.getHeaderSize(), (int) thatSend);
                IOUtil.write(mOut, dataEnc);
                read = mInput.read();
                if (read == LCmd.FS_BREAK) {
                    thatSend = 0;
                    T.s("发送文件：" + content.getContent() + " 被中断");
                    break;
                }
                if (!content.isTransfer()) {
                    thatSend = -3;
                    break;
                }
                totalSend += ten;
                thatSend += ten;
                int progress = (int) (totalSend * 100 / totalLength);
                if (progress != p) {
                    content.setProgress(progress);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_PROGRESS;
                    mMessage.obj = content;
                    messageSend(mMessage);
                    p = progress;
                }
            }
        } catch (IOException e) {
            logger.error("error: ", e);
            thatSend = 0;
        }
        dataEnc.reset();
        if (thatSend != fileLength) {
            dataEnc.setByteCmd(LCmd.FS_CLOSE);
        } else {
            dataEnc.setByteCmd(LCmd.FS_END);
        }
        try {
            IOUtil.write(mOut, dataEnc);
        } catch (IOException e) {
            logger.error("error: ", e);
        }
        return thatSend;
    }

    /**
     * @return List<Device>
     * @author fgsq
     * @comments 获取自己的设备信息
     * @date 2024/5/22 10:07
     */
    public List<Device> getSelfDevices() {
//        long startTime = System.currentTimeMillis();
        List<NetInfo> netInfoList = NetWorkUtil.getNetInfoList();
        List<Device> mDeviceList = getDevices(netInfoList);
        String ip = "";
        if (!netInfoList.isEmpty()) {
            ip = "http://" + netInfoList.get(0).getIp();
        } else if (!ipv6NetInfoList.isEmpty()) {
            ip = "http://[" + ipv6NetInfoList.get(0).getIp();
        }
        if (!ip.equals(currentIp)) {
            Message mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_NETWORK_CHANGES;
            mMessage.obj = ip;
            messageSend(mMessage);
            currentIp = ip;
        }
//        logger.debug(info("mDeviceList: " + mDeviceList.size());
//        logger.debug(info("time: " + (System.currentTimeMillis() - startTime) + "ms");
        return mDeviceList;
    }

    private static List<Device> getDevices(List<NetInfo> netInfoList) {
        int[] batteryLevel = BatteryUtil.getBatteryLevel(LANService.getInstance());
        List<Device> mDeviceList = new ArrayList<>();
        for (NetInfo netInfo : netInfoList) {
            Device device = new Device();
            device.setDevName(Config.USER_NAME);
            device.setDevIP(netInfo.getIp());
            device.setDevNetMask(netInfo.getMask());
            device.setDevBrotIP(netInfo.getBrodIp());
            device.setDevPort(Config.FILE_SERVER_PORT);
            device.setDevMode(Device.ANDROID);
            device.setDataVersion(Config.DATA_VERSION);
            device.setInterfaceName(netInfo.getInterfaceName());
            device.setBatteryLevel(batteryLevel[0]);
            device.setChargeStatus((byte) batteryLevel[1]);
            mDeviceList.add(device);
//            logger.debug( netInfoList.size() + " " + netInfo.getIp() + " " + netInfo.getBrodIp() + " " + netInfo.getMask());
        }
        return mDeviceList;
    }

    public String getDevName() {
        return App.getPrefUtil().getString(PreConfig.USER_NAME);
    }

    /**
     * @author fgsq
     * @comments 发送当前设备状态
     * @date 2024/4/29 11:32
     */
    public void noticeDeviceStateByIp(Device fromDevice, boolean isBroadcast, boolean isOnLine) {
        String fromIp = isBroadcast ? fromDevice.getDevBrotIP() : fromDevice.getDevIP();
        for (Device toDevice : localDevices) {
            if (NetWorkUtil.subNet(toDevice.getDevIP(), fromIp, toDevice.getDevNetMask())) {
                DataEnc dataEnc = makeDataEncUdp(toDevice, fromDevice, 1024);
                dataEnc.setCmd(isOnLine ? LCmd.UDP_SET_DEVICES : LCmd.UDP_DEVICES_OFF_LINE);
                try {
                    DatagramSocket datagramSocket;
                    if (isBroadcast) {
                        multicastLock.acquire();
                        datagramSocket = new MulticastSocket();
                        ((MulticastSocket) datagramSocket).setNetworkInterface(NetworkInterface.getByName(fromDevice.getInterfaceName()));
                    } else {
                        datagramSocket = new DatagramSocket();
                    }
                    UDPTools.sendData(datagramSocket, dataEnc, fromIp, Config.UDP_PORT);
                } catch (IOException e) {
                    logger.error("error: ", "noticeDeviceOnLineByIp error:", e);
                } finally {
                    if (isBroadcast) {
                        multicastLock.release();
                    }
                }
                dataEnc.reset();
            }
        }
    }

 /*   public void noticeDeviceMuteMedia() {
        for (Device device : onLineDevices.values()) {
            for (Device localDev : localDevices) {
                if (NetWorkUtil.subNet(device.getDevIP(), localDev.getDevIP(), localDev.getDevNetMask())) {
                    DataEnc dataEnc = makeDataEncUdp(localDev, 2048);
                    dataEnc.setCmd(LCmd.UDP_SEND_MEDIA_MUTE);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, device.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        logger.error("error: ",e);
                    }
                    break;
                }
            }
        }
    }

    public void noticeDeviceRestoreMedia() {
        for (Device device : onLineDevices.values()) {
            for (Device localDev : localDevices) {
                if (NetWorkUtil.subNet(device.getDevIP(), localDev.getDevIP(), localDev.getDevNetMask())) {
                    DataEnc dataEnc = makeDataEncUdp(localDev, 2048);
                    dataEnc.setCmd(LCmd.UDP_SEND_MEDIA_RESTORE);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, device.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        logger.error("error: ",e);
                    }
                    break;
                }
            }
        }
    }*/

    // 文件接收服务
    public void tcpServer() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(true); // 设置为阻塞模式
                serverSocketChannel.bind(new InetSocketAddress(Config.FILE_SERVER_PORT));
                fileReceive = serverSocketChannel.socket(); // 获取ServerSocket
            } else {
                fileReceive = new ServerSocket(Config.FILE_SERVER_PORT);

            }
        } catch (IOException e) {
            logger.error("error: ", e);
            T.ss((R.string.start_tcp_service_failed));
        }
        ThreadUtils.runThread(() -> {
            while (running) {
                Socket client;
                try {
                    // 等待客户端连接
                    client = fileReceive.accept();
                } catch (IOException e) {
                    logger.error("error: ", e);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                ThreadUtils.runThread(() -> {
                    try {
                        byte[] magicBytes = new byte[4];
                        InputStream is = client.getInputStream();
                        OutputStream out = client.getOutputStream();
                        if (is.read(magicBytes) != 4) return;
                        int magicNum = ByteUtil.bytesToInt(magicBytes, 0);
                        // 版本兼容，魔法数字判断
                        if (magicNum == Config.MAGIC_NUM) {
                            /* 处理自定义协议消息 */
                            handleTcp(client, new CustomDataInputStream(is), new CustomDataOutputStream(out));
                        } else if (Config.WEB_SERVICE) {
                            /* 处理HTTP服务 */
                            String http = new String(magicBytes);
                            String magicStr = http.toUpperCase();
                            if (magicStr.contains("GET") || magicStr.contains("POST")) {
                                httpServer.getHttpServer().handleWebClient(client, magicStr);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("error: ", e);
                        try {
                            // 忘了为什么加延时，不敢删除
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {
                        }
                    }
                });
            }
        });
    }


    private DatagramSocket ipGetSocket = null;
//    private MulticastSocket multicastSocket = null;

    // 监听并处理获取客户和设置客户端命令
    public void udpServer() {
        ThreadUtils.runThread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                ipGetSocket = new DatagramSocket(null);
//                ipGetSocket.setReuseAddress(true);
                ipGetSocket.setBroadcast(true);
                ipGetSocket.bind(new InetSocketAddress(Config.UDP_PORT));
//                multicastSocket = new MulticastSocket(Config.UDP_PORT);
//                MulticastSocket multicastSocket = new MulticastSocket();
                // 获取网络接口列表
//                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//                while (interfaces.hasMoreElements()) {
//                    NetworkInterface networkInterface = interfaces.nextElement();
//                    // 检查网络接口是否支持多播
//                    if (networkInterface.isUp() && networkInterface.supportsMulticast()) {
//                        // 绑定到该接口
//                        multicastSocket.setNetworkInterface(networkInterface);
//                        break;
//                    }
//                }
                logger.debug("runReceive running");
            } catch (IOException e) {
                logger.error("error: ", e);
                T.ss((R.string.start_udp_service_failed));
                return;
            }
            while (running) {
                try {
                    multicastLock.acquire();
                    ipGetSocket.receive(packet);
//                    multicastSocket.receive(packet);
                } catch (Exception e) {
                    logger.error("error: ", "udp receive error:", e);
                } finally {
                    multicastLock.release();
                }
                byte[] data = packet.getData();
                int len = packet.getLength();
                // 去除无效数据包
                if (data.length < 12) {
                    return;
                }
                int magicNum = ByteUtil.bytesToInt(data, 0);
                if (magicNum == Config.MAGIC_NUM) {
                    ThreadUtils.runThread(() -> {
                        byte[] buff = new byte[data.length - 4];
                        System.arraycopy(data, 4, buff, 0, buff.length);
                        try {
                            handleUdp(buff, len - 4);
                        } catch (IOException e) {
                            logger.error("error: ", e);
                        }
                    });
                }
            }
        });
    }

    private void handleUdp(byte[] buff, int len) throws IOException {
        CustomDataInputStream input = new CustomDataInputStream(new ByteArrayInputStream(buff));
        input.readInt();
        Device device;
        DataDec dataDec = new DataDec(buff, len);
        dataDec.decAllData();
        int cmd = dataDec.getCmd();
        // 设备端口
        int devPort = dataDec.getInt();
        // 设备ip
        String devIp = dataDec.getString();
        // 设备名
        String devName = dataDec.getString();
        // 设备类型
        int devMode = dataDec.getInt();
        // 设备唯一码
        String uniqueUUid = dataDec.getString();
        // dataVersion
        int dataVersion = dataDec.getInt();
        // 电量
        int batteryLevel = dataDec.getInt();
        // 充电状态
        byte chargeStatus = dataDec.getByte();
        // 排除自己发送的数据包
        for (Device dev : localDevices) {
            if (devIp != null && devIp.equals(dev.getDevIP())) {
                return;
            }
        }
        if (uniqueUUid != null && uniqueUUid.contains("-")) {
            try {
                String[] split = uniqueUUid.split("-");
                dataVersion = Integer.parseInt(split[1]);
                uniqueUUid = split[0];
            } catch (Exception e) {
                logger.error("error: ", e);
            }
        }
        device = new Device();
        device.setDevPort(devPort);
        device.setDevIP(devIp);
        device.setDevName(devName);
        device.setDevMode(devMode);
        device.setSetTime(System.currentTimeMillis());
        device.setDataVersion(dataVersion);
        device.setUniqueUUid(uniqueUUid);
        device.setBatteryLevel(batteryLevel);
        device.setChargeStatus(chargeStatus);
        addDevice(device);
        handleUdpCmd(device, cmd, dataDec);
    }

    private void handleUdpCmd(Device device, int cmd, DataDec dataDec) {
        String address = device.getDevIP() + ":" + device.getDevPort();
        if (cmd == LCmd.UDP_GET_DEVICES) {
            // 向设备发送自己的数据
            noticeDeviceStateByIp(device, false, true);
        }/* else if (cmd == LCmd.UDP_SET_DEVICES) {
            // 通讯协议版本
//            Log.d("dataVersion", "" + dataVersion);
            // 排除自己发送的数据
        } */ else if (cmd == LCmd.UDP_DEVICES_OFF_LINE) {
            if (device.isCanRemove()) {
                removeDevice(address);
            }
        } else if (cmd == LCmd.UDP_DEVICES_MESSAGE) {
            String messageEnc = dataDec.getString();
            String packageName = dataDec.getString();

            try {
                String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
                // 广播消息
                broadcastMessage(true, device.getDevIP(), message, device.getDevName(), packageName);
                // webSocket
                LHttpServer.sendMessage(message, device.getDevName(), "", 0, 0, "", true, false);
                MessageContent content = new MessageContent();
                content.setId(StringUtils.getUUID());
                content.setStatus(MessageContent.SUCCESS);
                content.setUserName(device.getDevName());
                content.setContent(message);
                content.setLeft(true);
                content.setDevMode(device.getDevMode());
                addMessage(content);
                logger.debug(message);
            } catch (Exception e) {
                logger.error("error: ", e);
                T.s((R.string.message_decryption_failed));
            }
        } else if (cmd == LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD) {
            String messageEnc = dataDec.getString();
            String packageName = dataDec.getString();
            try {
                String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
                broadcastMessage(true, device.getDevIP(), message, device.getDevName(), packageName);
                LHttpServer.sendMessage(message, device.getDevName(), "", 0, 0, "", true, true);
                MessageContent content = new MessageContent();
                content.setId(StringUtils.getUUID());
                content.setStatus(MessageContent.SUCCESS);
                content.setUserName(device.getDevName());
                content.setContent(message);
                content.setLeft(true);
                content.setDevMode(device.getDevMode());
                addMessage(content);
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("text", message));
                T.s((R.string.copy_text_to_clipboard_successful));
            } catch (Exception e) {
                logger.error("error: ", e);
                T.s((R.string.message_decryption_failed));
            }
        } else if (cmd == LCmd.UDP_SEND_MAP) {
            String content = dataDec.getString();
            MessageGPSContent messageGPSContent = new MessageGPSContent(content);
            messageGPSContent.setLeft(true);
            messageGPSContent.setId(StringUtils.getUUID());
            addMessage(messageGPSContent);
        }
        /*else if (cmd == LCmd.UDP_SEND_MEDIA_MUTE) {
            if (App.getPrefUtil().getBoolean(PreConfig.RECEIVE_MUTE)) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    logger.debug( "设置静音");
                }
            }
        } else if (cmd == LCmd.UDP_SEND_MEDIA_RESTORE) {
            if (App.getPrefUtil().getBoolean(PreConfig.RECEIVE_MUTE)) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, //音量类型
                            systemVolume, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    logger.debug( "取消静音");
                }
            }
        }*/
    }


    private void broadcastMessage(boolean isClip, String devIp, String message, String devName, String packageName) {
        try {
            boolean broadcastMessage = App.getPrefUtil().getBoolean(PreConfig.BROADCAST_MESSASGE);
            if (broadcastMessage && !StringUtils.isEmpty(packageName) && !StringUtils.isEmpty(message)) {
                // 广播消息
                Intent newIntent = new Intent(LANShareReceiver.LANSHARE_BORADCAST_EXPORT_INERFACE);
                newIntent.setPackage(packageName);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", devIp);
                jsonObject.put("msg", message);
                jsonObject.put("isClip", isClip);
                jsonObject.put("devName", devName);
                newIntent.putExtra("content", jsonObject.toString());
                sendBroadcast(newIntent);
            }
        } catch (Exception e) {
            logger.error("error: ", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.debug("onDestroy 退出");
        running = false;
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, false);
            }
        });
        if (Config.MEDIA_SYNC) {
            imageObserver.unregisterObserver();
        }
        IOUtil.closeIO(ipGetSocket, fileReceive);
//        IOUtil.closeIO(multicastSocket, fileRecive);
        unregisterReceiver(netWorkReceiver);
    }

    public Socket makeSocket(Device device) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                socketChannel.connect(new InetSocketAddress(device.getDevIP(), device.getDevPort()));

                Socket socket = socketChannel.socket();
                CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
                outputStream.writeInt(Config.MAGIC_NUM);
                outputStream.flush();
                // 保存channel引用以便后续关闭
                socket.getChannel(); // 这里可以保存引用
                return socket;
            } catch (IOException e) {
                // 如果创建失败，确保关闭channel
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (IOException closeException) {
                        logger.warn("Failed to close socket channel", closeException);
                    }
                }
                throw e;
            }
        } else {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(device.getDevIP(), device.getDevPort()), 4000);
            CustomDataOutputStream outputStream = new CustomDataOutputStream(socket.getOutputStream());
            // 魔法数
            outputStream.writeInt(Config.MAGIC_NUM);
            outputStream.flush();
            return socket;
        }

    }

    public Device makeIPv6Device() {
        Device device = new Device();
        device.setDevName(App.getPrefUtil().getString(PreConfig.USER_NAME));
        if (ipv6NetInfoList == null || ipv6NetInfoList.isEmpty()) {
            device.setDevIP("");
        } else {
            device.setDevIP(ipv6NetInfoList.get(0).getIp());
        }
        device.setDevNetMask("");
        device.setDevBrotIP("");
        device.setDevPort(Config.FILE_SERVER_PORT);
        device.setDevMode(Device.ANDROID);
        device.setDataVersion(Config.DATA_VERSION);
        return device;
    }

    public Socket makeSocket(InetAddress host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        OutputStream outputStream = socket.getOutputStream();
        byte[] magicBytes = ByteUtil.intToBytes(Config.MAGIC_NUM);
        // 数据协议版本
        outputStream.write(magicBytes);
        outputStream.flush();
        return socket;
    }

    public DataEnc makeDataEncUdp(Device device, Device toDevice, int size) {
        DataEnc dataEnc = new DataEnc(size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        // 兼容之前写的屎山代码
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(LVersion.DATA_VERSION_3);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    public DataEnc makeDataEnc(Device device, Device toDevice, int size) {
        DataEnc dataEnc = new DataEnc(size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        // 兼容之前写的屎山代码
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(LVersion.DATA_VERSION_3);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    public DataEnc makeDataEncCustomBuffer(Device device, byte[] buff, int size) {
        DataEnc dataEnc = new DataEnc(buff, size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        // 兼容之前写的屎山代码
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(Config.DATA_VERSION);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    public Device getDevice(Device device) {
        for (Device fromDevice : localDevices) {
            if (!device.isIPv4() || NetWorkUtil.subNet(device.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                return fromDevice;
            }
        }
        return null;
    }

    // 局域网扫描设备
    public void scanDevices() {
        logger.debug("scanDevices：" + running);
        ThreadUtils.runThread(() -> {
            while (running) {
                localDevices.clear();
                localDevices.addAll(getSelfDevices());
                for (Device selfDevice : localDevices) {
                    try {
                        if (!StringUtils.isEmpty(selfDevice.getDevBrotIP())) {
                            DataEnc dataEnc = makeDataEncUdp(selfDevice, null, 1024);
                            dataEnc.setCmd(LCmd.UDP_GET_DEVICES);
                            UDPTools.sendData(new DatagramSocket(), dataEnc, selfDevice.getDevBrotIP(), Config.UDP_PORT);
                        }
                        long currentTime = System.currentTimeMillis();
                        // 移除没有心跳的设备
                        for (String key : onLineDevices.keySet()) {
                            Device device = onLineDevices.get(key);
                            if (device != null && device.isCanRemove()) {
                                long setTime = device.getSetTime();
                                long timeOut = currentTime - setTime;
                                // 超过20秒没有心跳的设备直接移除
                                if (timeOut > (1000 * 20)) {
                                    removeDevice(key);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    public void broadcastMessage(Device toDevice, String message, boolean isClip) {
        broadcastMessage(toDevice, message, isClip, "", true);
    }

    public void broadcastMessage(Device toDevice, String message, boolean isClip, String packageName) {
        broadcastMessage(toDevice, message, isClip, packageName, true);
    }

    /**
     * 发送消息
     *
     * @param toDevice    发送给哪个设备，为空则发送给所有设备
     * @param msg         发送的内容
     * @param isClip      写入剪切板状态
     * @param packageName 广播消息时指定接收广播的应用包名（高版本安卓必须指定包名）
     * @param shareWS     是否分享消息内容给websocket网页聊天界面
     */
// 广播发送信息
    public void broadcastMessage(Device toDevice, String msg, boolean isClip, String packageName, boolean shareWS) {
        String message;
        try {
            message = AESUtils.encrypt(msg, Config.MESSAGE_KEY);
        } catch (Exception e) {
            logger.error("error: ", "encrypt msg error", e);
            T.s((R.string.message_encryption_failed));
            return;
        }
        logger.debug("broadcastMessage:" + message);
        if (shareWS) {
            if (toDevice == null) {
                ThreadUtils.runThread(() -> {
                    LHttpServer.sendMessage(
                            msg, getString(R.string.all_devices) + " ← " + getDevName(),
                            "", 0, 0, "", false, isClip
                    );
                });
            } else if (toDevice.getDevMode() == Device.WEB) {
                ThreadUtils.runThread(() -> LHttpServer.sendMessage(
                        toDevice.getWebSocketServer(), msg,
                        toDevice.getDevName() + " ← " + getDevName(), "",
                        0, toDevice.getDevMode(), "", false, isClip
                ));
                return;
            }
        }
        if (msg.length() > 700) {
            T.s(R.string.text_length_exceeds_limit);
            ThreadUtils.runThread(() -> {
                if (toDevice == null) {
                    for (Device dev : onLineDevices.values()) {
                        if (dev.isIPv4()) {
                            for (Device fromDevice : localDevices) {
                                if (NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                    if (dev.getDataVersion() >= LVersion.DATA_VERSION_4) {
                                        fileSend.sendMessage(dev, message, isClip);
                                        break;
                                    }
                                    DataEnc dataEnc = makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                                    dataEnc.setCmd(LCmd.FS_MESSAGE);
                                    dataEnc.putString(message);
                                    dataEnc.putString(packageName);
                                    Socket socket = null;
                                    try {
                                        socket = makeSocket(dev);
                                        OutputStream outputStream = socket.getOutputStream();
                                        IOUtil.write(outputStream, dataEnc);
                                        IOUtil.closeIO(outputStream, socket);
                                    } catch (Exception e) {
                                        logger.error("error: ", e);
                                    }
                                    break;
                                }

                            }
                        } else {
                            if (dev.getDataVersion() >= LVersion.DATA_VERSION_4) {
                                fileSend.sendMessage(dev, message, isClip);
                                continue;
                            }
                            DataEnc dataEnc = makeDataEncUdp(makeIPv6Device(), null, 1024 + message.getBytes().length);
                            dataEnc.setCmd(LCmd.FS_MESSAGE);
                            dataEnc.putString(message);
                            dataEnc.putString(packageName);
                            Socket socket = null;
                            try {
                                socket = makeSocket(dev);
                                OutputStream outputStream = socket.getOutputStream();
                                IOUtil.write(outputStream, dataEnc);
                                IOUtil.closeIO(outputStream, socket);
                            } catch (Exception e) {
                                logger.error("error: ", e);
                            }
                        }
                    }
                } else {
                    if (toDevice.isIPv4()) {
                        for (Device fromDevice : localDevices) {
                            // 指定了设备单独发送
                            if (NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                if (toDevice.getDataVersion() >= LVersion.DATA_VERSION_4) {
                                    fileSend.sendMessage(toDevice, message, isClip);
                                    break;
                                }
                                DataEnc dataEnc = makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                                dataEnc.setCmd(LCmd.FS_MESSAGE);
                                dataEnc.putString(message);
                                dataEnc.putString(packageName);
                                Socket socket = null;
                                try {
                                    socket = makeSocket(toDevice);
                                    OutputStream outputStream = socket.getOutputStream();
                                    IOUtil.write(outputStream, dataEnc);
                                    IOUtil.closeIO(outputStream, socket);
                                } catch (Exception e) {
                                    logger.error("error: ", e);
                                }
                                break;
                            }
                        }
                    } else {
                        if (toDevice.getDataVersion() >= LVersion.DATA_VERSION_4) {
                            fileSend.sendMessage(toDevice, message, isClip);
                            return;
                        }
                        DataEnc dataEnc = makeDataEncUdp(makeIPv6Device(), null, 1024 + message.getBytes().length);
                        dataEnc.setCmd(LCmd.FS_MESSAGE);
                        dataEnc.putString(message);
                        dataEnc.putString(packageName);
                        Socket socket = null;
                        try {
                            socket = makeSocket(toDevice);
                            OutputStream outputStream = socket.getOutputStream();
                            IOUtil.write(outputStream, dataEnc);
                            IOUtil.closeIO(outputStream, socket);
                        } catch (Exception e) {
                            logger.error("error: ", e);
                        }
                    }
                }

            });
            return;
        }
        ThreadUtils.runThread(() -> {
            if (toDevice == null) {
                for (Device dev : onLineDevices.values()) {
                    if (dev.isIPv4()) {
                        for (Device fromDevice : localDevices) {
                            if (!dev.isIPv4() || NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                DataEnc dataEnc = makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                                dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                                dataEnc.putString(message);
                                dataEnc.putString(packageName);
                                try {
                                    UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
                                } catch (IOException e) {
                                    logger.error("error: ", e);
                                }
                                logger.debug("onLineDevices:" + dev.getDevName() + " " + dev.getDevIP());
                                break;
                            }
                        }
                    } else {
                        DataEnc dataEnc = makeDataEncUdp(makeIPv6Device(), null, 1024 + message.getBytes().length);
                        dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                        dataEnc.putString(message);
                        dataEnc.putString(packageName);
                        try {
                            UDPTools.sendData(new DatagramSocket(), dataEnc, dev.getDevIP(), Config.UDP_PORT);
                        } catch (IOException e) {
                            logger.error("error: ", e);
                        }
                    }
                }
            } else {
                if (toDevice.isIPv4()) {
                    for (Device fromDevice : localDevices) {
                        if (NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                            DataEnc dataEnc = makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                            dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                            dataEnc.putString(message);
                            dataEnc.putString(packageName);
                            try {
                                UDPTools.sendData(new DatagramSocket(), dataEnc, toDevice.getDevIP(), Config.UDP_PORT);
                            } catch (IOException e) {
                                logger.error("error: ", e);
                            }
                            break;
                        }
                    }
                } else {
                    DataEnc dataEnc = makeDataEncUdp(makeIPv6Device(), null, 1024 + message.getBytes().length);
                    dataEnc.setCmd(isClip ? LCmd.UDP_DEVICES_MESSAGE_TO_CLIPBOARD : LCmd.UDP_DEVICES_MESSAGE);
                    dataEnc.putString(message);
                    dataEnc.putString(packageName);
                    try {
                        UDPTools.sendData(new DatagramSocket(), dataEnc, toDevice.getDevIP(), Config.UDP_PORT);
                    } catch (IOException e) {
                        logger.error("error: ", e);
                    }
                }
            }
        });

    }

    public void checkAndAddChatTime(String bindId) {
        if (DateUtils.isFiveMinutesAgo(Config.lastMessageTime)) {
            MessageTimeContent messageTimeContent = new MessageTimeContent();
            messageTimeContent.setId(StringUtils.getUUID());
            messageTimeContent.setBindId(bindId);
            if (Config.SAVE_MESSAGE) {
                messsageDButil.addMessage(messageTimeContent);
            }
            Config.lastMessageTime = messageTimeContent.getCreateTime().getTime();
        }
    }

    /**
     * 发送是否接收文件请求
     **/
    public void sendIfReceiveFilesMsg(RecvFileCallback recvFileCallback, int count) {
        // 弹出是否接收文件请求弹窗
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
        mMessage.arg1 = count;
        mMessage.obj = recvFileCallback;
        messageSend(mMessage);
    }

    /**
     * 发送进度
     **/
    public void sendProgressMeg(MessageFileContent fileContent) {
        Message mMessage = Message.obtain();
        mMessage.what = LCmd.SERVICE_PROGRESS;
        mMessage.obj = fileContent;
        messageSend(mMessage);
    }

    /**
     * 关闭进度
     **/
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
     * 显示进度
     **/
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

    /**
     * 添加消息
     **/
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
     * 获取数据库
     */
    public MediaIdPathDBUtil getMediaIdPathDBUtil() {
        return mediaIdPathDBUtil;
    }

}



















