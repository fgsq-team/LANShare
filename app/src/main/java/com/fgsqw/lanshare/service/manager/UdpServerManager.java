package com.fgsqw.lanshare.service.manager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.httpserver.utils.ByteUtil;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageGPSContent;
import com.fgsqw.lanshare.receiver.LANShareReceiver;
import com.fgsqw.lanshare.service.CustomDataInputStream;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * UDP 服务器管理器
 * <p>负责 UDP 广播、设备发现、消息广播</p>
 * <p>主要功能:</p>
 * <ul>
 *   <li>监听 UDP 广播</li>
 *   <li>处理设备发现</li>
 *   <li>广播消息到局域网设备</li>
 * </ul>
 *
 * @author fgsq
 * @version 1.0
 */
public class UdpServerManager {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(UdpServerManager.class);

    /** LAN 服务实例 */
    private final LANService service;

    /** 设备管理器 */
    private final DeviceManager deviceManager;

    /** UDP Socket */
    private DatagramSocket ipGetSocket = null;

    /** 运行状态标志 */
    private boolean running = true;

    /**
     * 构造函数
     *
     * @param service       LAN 服务实例
     * @param deviceManager 设备管理器
     */
    public UdpServerManager(LANService service, DeviceManager deviceManager) {
        this.service = service;
        this.deviceManager = deviceManager;
    }

    /**
     * 启动 UDP 服务器
     * <p>开始监听 UDP 广播消息</p>
     */
    public void startServer() {
        ThreadUtils.runThread(() -> {
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                ipGetSocket = new DatagramSocket(null);
                ipGetSocket.setBroadcast(true);
                ipGetSocket.bind(new InetSocketAddress(Config.UDP_PORT));
                logger.debug("runReceive running");
            } catch (IOException e) {
                logger.error("error: ", e);
                T.ss((R.string.start_udp_service_failed));
                return;
            }
            while (running) {
                try {
                    deviceManager.getMulticastLock().acquire();
                    ipGetSocket.receive(packet);
                } catch (Exception e) {
                    logger.error("error: ", "udp receive error:", e);
                } finally {
                    deviceManager.getMulticastLock().release();
                }
                byte[] data = packet.getData();
                int len = packet.getLength();
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

    /**
     * 处理 UDP 数据
     *
     * @param buff 数据缓冲区
     * @param len  数据长度
     * @throws IOException 如果发生 I/O 错误
     */
    private void handleUdp(byte[] buff, int len) throws IOException {
        CustomDataInputStream input = new CustomDataInputStream(new ByteArrayInputStream(buff));
        input.readInt();
        Device device;
        DataDec dataDec = new DataDec(buff, len);
        dataDec.decAllData();
        int cmd = dataDec.getCmd();
        int devPort = dataDec.getInt();
        String devIp = dataDec.getString();
        String devName = dataDec.getString();
        int devMode = dataDec.getInt();
        String uniqueUUid = dataDec.getString();
        int dataVersion = dataDec.getInt();
        int batteryLevel = dataDec.getInt();
        byte chargeStatus = dataDec.getByte();

        // 排除自己发送的数据包
        for (Device dev : deviceManager.localDevices) {
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
        deviceManager.addDevice(device);
        handleUdpCmd(device, cmd, dataDec);
    }

    /**
     * 处理 UDP 命令
     *
     * @param device  发送方设备
     * @param cmd     命令类型
     * @param dataDec 数据解码器
     */
    private void handleUdpCmd(Device device, int cmd, DataDec dataDec) {
        String address = device.getDevIP() + ":" + device.getDevPort();
        if (cmd == LCmd.UDP_GET_DEVICES) {
            deviceManager.noticeDeviceStateByIp(device, false, true);
        } else if (cmd == LCmd.UDP_DEVICES_OFF_LINE) {
            if (device.isCanRemove()) {
                deviceManager.removeDevice(address);
            }
        } else if (cmd == LCmd.UDP_DEVICES_MESSAGE) {
            String messageEnc = dataDec.getString();
            String packageName = dataDec.getString();
            try {
                String message = AESUtils.decrypt(messageEnc, Config.MESSAGE_KEY);
                broadcastMessage(true, device.getDevIP(), message, device.getDevName(), packageName);
                LHttpServer.sendMessage(message, device.getDevName(), "", 0, 0, "", true, false);
                MessageContent content = new MessageContent();
                content.setId(StringUtils.getUUID());
                content.setStatus(MessageContent.SUCCESS);
                content.setUserName(device.getDevName());
                content.setContent(message);
                content.setLeft(true);
                content.setDevMode(device.getDevMode());
                service.addMessage(content);
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
                service.addMessage(content);
                ClipboardManager cb = (ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE);
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
            service.addMessage(messageGPSContent);
        }
    }

    /**
     * 广播消息
     *
     * @param isClip      是否写入剪贴板
     * @param devIp       发送方 IP
     * @param message     消息内容
     * @param devName     发送方设备名
     * @param packageName 包名
     */
    private void broadcastMessage(boolean isClip, String devIp, String message, String devName, String packageName) {
        try {
            boolean broadcastMessage = App.getPrefUtil().getBoolean(PreConfig.BROADCAST_MESSASGE);
            if (broadcastMessage && !StringUtils.isEmpty(packageName) && !StringUtils.isEmpty(message)) {
                Intent newIntent = new Intent(LANShareReceiver.LANSHARE_BORADCAST_EXPORT_INERFACE);
                newIntent.setPackage(packageName);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ip", devIp);
                jsonObject.put("msg", message);
                jsonObject.put("isClip", isClip);
                jsonObject.put("devName", devName);
                newIntent.putExtra("content", jsonObject.toString());
                service.sendBroadcast(newIntent);
            }
        } catch (Exception e) {
            logger.error("error: ", e);
        }
    }

    /**
     * 发送消息到设备
     *
     * @param toDevice    目标设备,如果为 null 则广播到所有设备
     * @param msg         消息内容
     * @param isClip      是否写入剪贴板
     * @param packageName 包名
     * @param shareWS     是否分享到 WebSocket
     */
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
                            msg, service.getString(R.string.all_devices) + " ← " + deviceManager.getDevName(),
                            "", 0, 0, "", false, isClip
                    );
                });
            } else if (toDevice.getDevMode() == Device.WEB) {
                ThreadUtils.runThread(() -> LHttpServer.sendMessage(
                        toDevice.getWebSocketServer(), msg,
                        toDevice.getDevName() + " ← " + deviceManager.getDevName(), "",
                        0, toDevice.getDevMode(), "", false, isClip
                ));
                return;
            }
        }
        if (msg.length() > 700) {
            T.s(R.string.text_length_exceeds_limit);
            ThreadUtils.runThread(() -> {
                if (toDevice == null) {
                    for (Device dev : deviceManager.getOnLineDevices().values()) {
                        if (dev.isIPv4()) {
                            for (Device fromDevice : deviceManager.localDevices) {
                                if (NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                    if (dev.getDataVersion() >= LVersion.DATA_VERSION_4) {
                                        service.getFileSend().sendMessage(dev, message, isClip);
                                        break;
                                    }
                                    DataEnc dataEnc = deviceManager.makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                                    dataEnc.setCmd(LCmd.FS_MESSAGE);
                                    dataEnc.putString(message);
                                    dataEnc.putString(packageName);
                                    Socket socket = null;
                                    try {
                                        socket = service.makeSocket(dev);
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
                                service.getFileSend().sendMessage(dev, message, isClip);
                                continue;
                            }
                            DataEnc dataEnc = deviceManager.makeDataEncUdp(deviceManager.makeIPv6Device(), null, 1024 + message.getBytes().length);
                            dataEnc.setCmd(LCmd.FS_MESSAGE);
                            dataEnc.putString(message);
                            dataEnc.putString(packageName);
                            Socket socket = null;
                            try {
                                socket = service.makeSocket(dev);
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
                        for (Device fromDevice : deviceManager.localDevices) {
                            if (NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                if (toDevice.getDataVersion() >= LVersion.DATA_VERSION_4) {
                                    service.getFileSend().sendMessage(toDevice, message, isClip);
                                    break;
                                }
                                DataEnc dataEnc = deviceManager.makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
                                dataEnc.setCmd(LCmd.FS_MESSAGE);
                                dataEnc.putString(message);
                                dataEnc.putString(packageName);
                                Socket socket = null;
                                try {
                                    socket = service.makeSocket(toDevice);
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
                            service.getFileSend().sendMessage(toDevice, message, isClip);
                            return;
                        }
                        DataEnc dataEnc = deviceManager.makeDataEncUdp(deviceManager.makeIPv6Device(), null, 1024 + message.getBytes().length);
                        dataEnc.setCmd(LCmd.FS_MESSAGE);
                        dataEnc.putString(message);
                        dataEnc.putString(packageName);
                        Socket socket = null;
                        try {
                            socket = service.makeSocket(toDevice);
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
                for (Device dev : deviceManager.getOnLineDevices().values()) {
                    if (dev.isIPv4()) {
                        for (Device fromDevice : deviceManager.localDevices) {
                            if (!dev.isIPv4() || NetWorkUtil.subNet(dev.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                                DataEnc dataEnc = deviceManager.makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
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
                        DataEnc dataEnc = deviceManager.makeDataEncUdp(deviceManager.makeIPv6Device(), null, 1024 + message.getBytes().length);
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
                    for (Device fromDevice : deviceManager.localDevices) {
                        if (NetWorkUtil.subNet(toDevice.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                            DataEnc dataEnc = deviceManager.makeDataEncUdp(fromDevice, null, 1024 + message.getBytes().length);
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
                    DataEnc dataEnc = deviceManager.makeDataEncUdp(deviceManager.makeIPv6Device(), null, 1024 + message.getBytes().length);
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

    /**
     * 停止 UDP 服务器
     */
    public void stop() {
        running = false;
        IOUtil.closeIO(ipGetSocket);
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
