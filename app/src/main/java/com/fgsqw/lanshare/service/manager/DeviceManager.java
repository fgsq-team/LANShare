package com.fgsqw.lanshare.service.manager;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Message;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.LVersion;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.*;
import com.fgsqw.lanshare.web.LHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 设备管理器
 * <p>负责设备发现、设备列表管理、网络状态管理</p>
 * <p>主要功能:</p>
 * <ul>
 *   <li>维护在线设备列表</li>
 *   <li>管理本机设备信息</li>
 *   <li>UDP 广播扫描设备</li>
 *   <li>发送设备状态通知</li>
 * </ul>
 *
 * @author fgsq
 * @version 1.0
 */
public class DeviceManager {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    /** 上下文 */
    private final Context context;
    
    /** LAN 服务实例 */
    private final LANService service;
    
    /** 在线设备列表,键为 "IP:Port" */
    private final Map<String, Device> onLineDevices = new ConcurrentHashMap<>();
    
    /** Web 设备列表 */
    public final Map<String, Device> onLineWebDevices = new ConcurrentHashMap<>();
    
    /** 本机设备信息列表 */
    public Set<Device> localDevices = Collections.synchronizedSet(new HashSet<>());
    
    /** IPv6 网络信息列表 */
    public List<NetInfo> ipv6NetInfoList = new ArrayList<>();
    
    /** 当前 IP 地址 */
    private String currentIp = "";
    
    /** 运行状态标志 */
    private boolean running = true;
    
    /** 多播锁,用于保持 WiFi 多播 */
    private WifiManager.MulticastLock multicastLock;

    /**
     * 构造函数
     *
     * @param context 上下文
     * @param service LAN 服务实例
     */
    public DeviceManager(Context context, LANService service) {
        this.context = context;
        this.service = service;
        initMulticastLock();
    }

    /**
     * 初始化多播锁
     */
    private void initMulticastLock() {
        android.net.wifi.WifiManager mWifiManager = 
            (android.net.wifi.WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        multicastLock = mWifiManager.createMulticastLock("multicastLock");
    }

    /**
     * 获取多播锁
     *
     * @return 多播锁
     */
    public WifiManager.MulticastLock getMulticastLock() {
        return multicastLock;
    }

    /**
     * 获取 IPv6 地址
     * <p>异步获取本机的 IPv6 地址信息</p>
     */
    public void getIpv6() {
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

    /**
     * 添加设备到在线列表
     *
     * @param device 要添加的设备
     */
    public void addDevice(Device device) {
        String address = device.getDevIP() + ":" + device.getDevPort();
        onLineDevices.put(address, device);
        LHttpServer.sendDeviceList();
    }

    /**
     * 从在线列表移除设备
     *
     * @param address 设备地址,格式为 "IP:Port"
     */
    public void removeDevice(String address) {
        onLineDevices.remove(address);
        LHttpServer.sendDeviceList();
    }

    /**
     * 获取在线设备列表
     *
     * @return 在线设备映射表
     */
    public Map<String, Device> getOnLineDevices() {
        return onLineDevices;
    }

    /**
     * 获取设备列表
     *
     * @return 设备列表
     */
    public List<Device> getDeviceList() {
        List<Device> deviceList;
        if (!onLineDevices.isEmpty()) {
            deviceList = new ArrayList<>(onLineDevices.values());
        } else {
            deviceList = new ArrayList<>();
        }
        return deviceList;
    }

    /**
     * 获取自己的设备信息
     * <p>根据当前网络状态生成本机设备信息列表</p>
     *
     * @return 本机设备列表
     */
    public List<Device> getSelfDevices() {
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
            service.messageSend(mMessage);
            currentIp = ip;
        }
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
        }
        return mDeviceList;
    }

    /**
     * 获取本机设备名
     *
     * @return 设备名
     */
    public String getDevName() {
        return App.getPrefUtil().getString(PreConfig.USER_NAME);
    }

    /**
     * 创建 IPv6 设备
     *
     * @return IPv6 设备信息
     */
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

    /**
     * 获取匹配的本机设备
     *
     * @param device 目标设备
     * @return 匹配的本机设备,如果未找到则返回 null
     */
    public Device getDevice(Device device) {
        for (Device fromDevice : localDevices) {
            if (!device.isIPv4() || NetWorkUtil.subNet(device.getDevIP(), fromDevice.getDevIP(), fromDevice.getDevNetMask())) {
                return fromDevice;
            }
        }
        return null;
    }

    /**
     * 发送设备状态通知
     *
     * @param fromDevice  发送方设备
     * @param isBroadcast 是否广播
     * @param isOnLine    是否在线
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
                        ((MulticastSocket) datagramSocket).setNetworkInterface(
                            NetworkInterface.getByName(fromDevice.getInterfaceName()));
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

    /**
     * 创建 UDP 数据包
     *
     * @param device   发送方设备
     * @param toDevice 目标设备
     * @param size     缓冲区大小
     * @return 数据包
     */
    public DataEnc makeDataEncUdp(Device device, Device toDevice, int size) {
        DataEnc dataEnc = new DataEnc(size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(LVersion.DATA_VERSION_3);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    /**
     * 创建数据包
     *
     * @param device   发送方设备
     * @param toDevice 目标设备
     * @param size     缓冲区大小
     * @return 数据包
     */
    public DataEnc makeDataEnc(Device device, Device toDevice, int size) {
        DataEnc dataEnc = new DataEnc(size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(LVersion.DATA_VERSION_3);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    /**
     * 创建自定义缓冲区数据包
     *
     * @param device 发送方设备
     * @param buff   缓冲区
     * @param size   缓冲区大小
     * @return 数据包
     */
    public DataEnc makeDataEncCustomBuffer(Device device, byte[] buff, int size) {
        DataEnc dataEnc = new DataEnc(buff, size);
        dataEnc.putInt(device.getDevPort());
        dataEnc.putString(device.getDevIP());
        dataEnc.putString(getDevName());
        dataEnc.putInt(Device.ANDROID);
        dataEnc.putString(Config.uniqueUUid + "-" + Config.DATA_VERSION);
        dataEnc.putInt(Config.DATA_VERSION);
        dataEnc.putInt(device.getBatteryLevel());
        dataEnc.putByte(device.getChargeStatus());
        return dataEnc;
    }

    /**
     * 初始化设备数据
     * <p>异步获取 IPv6 地址信息</p>
     */
    public void initData() {
        getIpv6();
    }

    /**
     * 局域网扫描设备
     * <p>定时广播设备在线状态,并清理超时设备</p>
     */
    public void scanDevices() {
        logger.debug("scanDevices：" + running);
        ThreadUtils.runThread(() -> {
            while (running) {
                localDevices.clear();
                localDevices.addAll(getSelfDevices());
                for (Device selfDevice : localDevices) {
                    try {
                        if (!com.fgsqw.lanshare.utils.StringUtils.isEmpty(selfDevice.getDevBrotIP())) {
                            DataEnc dataEnc = makeDataEncUdp(selfDevice, null, 1024);
                            dataEnc.setCmd(LCmd.UDP_GET_DEVICES);
                            UDPTools.sendData(new DatagramSocket(), dataEnc, 
                                selfDevice.getDevBrotIP(), Config.UDP_PORT);
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

    /**
     * 通知所有设备下线
     * <p>广播本机所有设备下线状态</p>
     */
    public void notifyAllDevicesOffline() {
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, false);
            }
        });
    }

    /**
     * 通知所有设备上线
     * <p>广播本机所有设备上线状态</p>
     */
    public void notifyAllDevicesOnline() {
        ThreadUtils.runThread(() -> {
            for (Device device : localDevices) {
                noticeDeviceStateByIp(device, true, true);
            }
        });
    }

    /**
     * 获取运行状态
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 设置运行状态
     *
     * @param running 运行状态
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}
