package com.fgsqw.lanshare.service.manager;

import android.os.Message;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.fragment.data.AnyData;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.service.version.four.FileServer;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.utils.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TCP 服务器管理器
 * <p>负责 TCP 连接和协议处理</p>
 * <p>主要功能:</p>
 * <ul>
 *   <li>监听 TCP 连接</li>
 *   <li>处理不同版本的协议</li>
 *   <li>解析命令并分发到对应处理器</li>
 * </ul>
 *
 * @author fgsq
 * @version 1.0
 */
public class TcpServerManager {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(TcpServerManager.class);

    /** LAN 服务实例 */
    private final LANService service;
    
    /** 设备管理器 */
    private final DeviceManager deviceManager;
    
    /** 文件服务器 */
    private final FileServer fileServer;
    
    /** TCP 服务器 Socket */
    private ServerSocket fileReceive;
    
    /** 运行状态标志 */
    private boolean running = true;

    /**
     * 构造函数
     *
     * @param service       LAN 服务实例
     * @param deviceManager 设备管理器
     * @param fileServer    文件服务器
     */
    public TcpServerManager(LANService service, DeviceManager deviceManager, FileServer fileServer) {
        this.service = service;
        this.deviceManager = deviceManager;
        this.fileServer = fileServer;
    }

    /**
     * 启动 TCP 服务器
     * <p>开始监听 TCP 连接请求</p>
     */
    public void startServer() {
        try {
            fileReceive = new ServerSocket(Config.FILE_SERVER_PORT);
            fileReceive.setReuseAddress(true); // 允许地址重用
        } catch (IOException e) {
            logger.error("error: ", e);
            T.ss((R.string.start_tcp_service_failed));
        }
        ThreadUtils.runThread(() -> {
            while (running) {
                Socket client;
                try {
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
                        if (magicNum == Config.MAGIC_NUM) {
                            handleTcp(client, new CustomDataInputStream(is), new CustomDataOutputStream(out));
                        } else if (Config.WEB_SERVICE) {
                            String http = new String(magicBytes);
                            String magicStr = http.toUpperCase();
                            if (magicStr.contains("GET") || magicStr.contains("POST")) {
                                service.getHttpServer().getHttpServer().handleWebClient(client, magicStr);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("error: ", e);
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {
                        }
                    }
                });
            }
        });
    }

    /**
     * 处理 TCP 连接
     *
     * @param client 客户端 Socket
     * @param input  输入流
     * @param out    输出流
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
                new DataEnc(buffer).setCmd(cmd);
                if (IOUtil.read(input, buffer, 4, DataEnc.getHeaderSize() - 4) != DataEnc.getHeaderSize() - 4)
                    return;
                dataDec = new DataDec(buffer, DataEnc.getHeaderSize());
                int length = dataDec.getLength();
                if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length) return;
                dataDec.setData(buffer, DataEnc.getHeaderSize() + length);
                int devPort = dataDec.getInt();
                String devIp = dataDec.getString();
                String devName = dataDec.getString();
                int devMode = dataDec.getInt();
                String uniqueUUid = dataDec.getString();
                int dataVersion = dataDec.getInt();
                int batteryLevel = dataDec.getInt();
                byte chargeStatus = dataDec.getByte();
                String address = devIp + ":" + devPort;
                device = deviceManager.getOnLineDevices().get(address);
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
            deviceManager.addDevice(device);
            handleVersion(cmd, dataDec, device, client, input, out);
        } catch (Exception e) {
            logger.error("handleTcp error", e);
        }
    }

    /**
     * 处理版本协议
     *
     * @param cmd      命令类型
     * @param dataDec  数据解码器
     * @param device   设备信息
     * @param client   客户端 Socket
     * @param input    输入流
     * @param out      输出流
     * @throws Exception 如果发生错误
     */
    private void handleVersion(int cmd, DataDec dataDec, Device device, Socket client, 
                               CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        if (cmd == LCmd.NEW_VERSION_4) {
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
                fsAddDevice(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_SHARE_FILE) {
                fsShareFile(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_MESSAGE) {
                fsMessage(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_GET_NO_SYNC_MEDIA) {
                fsGetMediaSync(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_UPDATE_APPS) {
                fsUpdateApps(dataDec, device, client, input, out);
            } else if (cmd == LCmd.FS_GET_APPS) {
                fsGetApps(dataDec, device, client, input, out);
            }
        } else {
            T.s("不支持的版本");
        }
    }

    /**
     * 添加设备命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     * @throws Exception 如果发生错误
     */
    private void fsAddDevice(DataDec dataDec, Device device, Socket client, 
                             CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        boolean isIPV6 = dataDec.getBool();
        device.setIPv4(!isIPV6);
        device.setCanRemove(false);
        deviceManager.addDevice(device);
        Device newDevice = null;
        if (isIPV6) {
            newDevice = deviceManager.makeIPv6Device();
        } else {
            for (Device localDevice : deviceManager.localDevices) {
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
        DataEnc dataEnc = deviceManager.makeDataEncCustomBuffer(newDevice, buffer, buffer.length);
        dataEnc.setCmd(LCmd.FS_ADD_DEVICE);
        dataEnc.putBool(isIPV6);
        IOUtil.write(out, dataEnc);
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
        }
        T.s(String.format(service.getString(R.string.add_device_successful), device.getDevName()));
        IOUtil.closeIO(input, out, client);
        com.fgsqw.lanshare.activity.DeviceQrCodeActivity.exitFlag = true;
    }

    /**
     * 接收文件命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     * @throws Exception 如果发生错误
     */
    private void fsShareFile(DataDec dataDec, Device device, Socket client, 
                             CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        byte[] buffer = new byte[1024 * 1024];
        int count = dataDec.getCount();
        boolean encData = dataDec.getBool();
        dataDec = new DataDec(buffer);
        List<MessageFileContent> fileContentList = new ArrayList<>();
        Message mMessage;
        for (int i = 0; i < count; i++) {
            if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
                throw new RuntimeException("read error");
            int length = dataDec.getLength();
            if (IOUtil.read(input, buffer, DataEnc.getHeaderSize(), length) != length)
                throw new RuntimeException("read error");
            dataDec.setData(buffer, buffer.length);
            long fileSize = dataDec.getLong();
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

        com.fgsqw.lanshare.service.version.four.FileTransfer fileTransfer = 
            new com.fgsqw.lanshare.service.version.four.FileTransfer();
        fileTransfer.setFiles(fileContentList);
        fileTransfer.setFromDevice(device);

        com.fgsqw.lanshare.service.RecvFileCallback recvFileCallback = 
            new com.fgsqw.lanshare.service.RecvFileCallback(fileTransfer, fileContentList, client, input, out, encData) {
            @Override
            public void receviceFile(boolean isAgree) {
                service.startReceivingFile(device, fileContentList, client, input, out, encData, isAgree);
            }
        };

        boolean isNotRecvDialog = com.fgsqw.lanshare.App.getPrefUtil().getBoolean(PreConfig.NOT_RECV_DIALOG, true);
        if (isNotRecvDialog) {
            recvFileCallback.receviceFile(true);
        } else {
            mMessage = Message.obtain();
            mMessage.what = LCmd.SERVICE_IF_RECIVE_FILES;
            mMessage.arg1 = count;
            mMessage.obj = recvFileCallback;
            service.messageSend(mMessage);
        }
    }

    /**
     * 接收消息命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     */
    private void fsMessage(DataDec dataDec, Device device, Socket client, 
                           CustomDataInputStream input, CustomDataOutputStream out) {
        try {
            String messageEnc = dataDec.getString();
            String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
            MessageContent content = new MessageContent();
            content.setId(StringUtils.getUUID());
            content.setStatus(MessageContent.SUCCESS);
            content.setUserName(device.getDevName());
            content.setContent(message);
            content.setLeft(true);
            service.addMessage(content);
        } catch (Exception e) {
            logger.error("message decrypt error:", e);
            T.s((R.string.message_decryption_failed));
        } finally {
            IOUtil.closeIO(out, input, client);
        }
    }

    /**
     * 获取媒体同步命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     * @throws Exception 如果发生错误
     */
    private void fsGetMediaSync(DataDec dataDec, Device device, Socket client, 
                                CustomDataInputStream input, CustomDataOutputStream out) throws Exception {
        int count = dataDec.getCount();
        byte[] buffer = new byte[1024 * 1024 * 2];
        DataEnc dataEnc = new DataEnc(buffer);
        dataEnc.setCount(count);
        int syncCount = 0;
        for (int i = 0; i < count; i++) {
            long mediaId = dataDec.getLong();
            if (!service.getMediaIdPathDBUtil().isIdExists(mediaId)) {
                syncCount++;
                dataEnc.putLong(mediaId);
            }
        }
        dataEnc.setCount(syncCount);
        IOUtil.write(out, dataEnc);
    }

    /**
     * 更新应用命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     * @throws Exception 如果发生错误
     */
    private void fsUpdateApps(DataDec dataDec, Device device, Socket client, 
                              InputStream input, OutputStream out) throws Exception {
        List<MessageApkContent> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            return;
        }
        byte[] buffer = new byte[1024 * 1024 * 2];
        dataDec = new DataDec(buffer);
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        int length = dataDec.getLength();
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
     * 获取应用命令处理
     *
     * @param dataDec 数据解码器
     * @param device  设备信息
     * @param client  客户端 Socket
     * @param input   输入流
     * @param out     输出流
     * @throws Exception 如果发生错误
     */
    private void fsGetApps(DataDec dataDec, Device device, Socket client, 
                           InputStream input, OutputStream out) throws Exception {
        List<MessageApkContent> apkFileList = AnyData.apkFileList;
        if (apkFileList == null || apkFileList.isEmpty()) {
            T.s("APP列表为空");
            return;
        }
        byte[] buffer = new byte[1024 * 1024];
        dataDec = new DataDec(buffer);
        if (IOUtil.read(input, buffer, 0, DataEnc.getHeaderSize()) != DataEnc.getHeaderSize())
            throw new RuntimeException("read error");
        int length = dataDec.getLength();
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
            service.messageSend(mMessage);
        }
    }

    /**
     * 停止 TCP 服务器
     */
    public void stop() {
        running = false;
        IOUtil.closeIO(fileReceive);
    }

    /**
     * 获取运行状态
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
