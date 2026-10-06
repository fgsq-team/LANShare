package com.fgsqw.lanshare.pojo;

import androidx.annotation.Nullable;


import com.alibaba.fastjson.JSONObject;
import com.fgsqw.httpserver.websocket.WebSocketServer;

import java.io.Serializable;

public class Device implements Serializable {
    public static final int UNKNOW = -1;
    public static final int ANDROID = 1;
    public static final int WINDOWS = 2;
    public static final int LINUX = 3;
    public static final int MAC_OS = 4;
    public static final int IOS = 5;
    public static final int HARMONY_OS = 6;
    public static final int WEB = 7;

    private String devName;    // 设备名称
    private String devIP;      // 设备ip
    private String devNetMask; // 子网掩码
    private String devBrotIP;  // 广播IP
    private String uniqueUUid;  // 设备唯一id
    private int devPort;       // 设备端口
    private int devMode = UNKNOW;       // 设备代号
    private long setTime;      // 设备最后心跳时间
    private int dataVersion = 0;  // 通讯协议版本
    private boolean isIPv4 = true; // 是否为ipv4设备
    private boolean canRemove = true; // 是否可以移除
    private String interfaceName;
    private int batteryLevel = -1;  // 电池电量
    private byte chargeStatus = -1;  // 电池电量

    WebSocketServer webSocketServer = null;

    public Device() {
    }

    public Device(String devName, String devIP, int devPort) {
        this.devName = devName;
        this.devIP = devIP;
        this.devPort = devPort;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getUniqueUUid() {
        return uniqueUUid;
    }

    public void setUniqueUUid(String uniqueUUid) {
        this.uniqueUUid = uniqueUUid;
    }

    public String getDevBrotIP() {
        return devBrotIP;
    }

    public void setDevBrotIP(String devBrotIP) {
        this.devBrotIP = devBrotIP;
    }

    public String getDevNetMask() {
        return devNetMask;
    }

    public void setDevNetMask(String devNetMask) {
        this.devNetMask = devNetMask;
    }

    public long getSetTime() {
        return setTime;
    }

    public void setSetTime(long setTime) {
        this.setTime = setTime;
    }

    public String getDevName() {
        return devName;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public String getDevIP() {
        return devIP;
    }

    public void setDevIP(String devIP) {
        this.devIP = devIP;
    }

    public int getDevPort() {
        return devPort;
    }

    public void setDevPort(int devPort) {
        this.devPort = devPort;
    }

    public int getDevMode() {
        return devMode;
    }

    public void setDevMode(int devMode) {
        this.devMode = devMode;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    public boolean isCanRemove() {
        return canRemove;
    }

    public void setCanRemove(boolean canRemove) {
        this.canRemove = canRemove;
    }

    public boolean isIPv4() {
        return isIPv4;
    }

    public boolean isIPv6() {
        return !isIPv4;
    }

    public void setIPv6(boolean IPv6) {
        isIPv4 = !IPv6;
    }


    public void setIPv4(boolean IPv4) {
        isIPv4 = IPv4;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public WebSocketServer getWebSocketServer() {
        return webSocketServer;
    }

    public void setWebSocketServer(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    /**
     * @return String
     * @author fgsq
     * @comments 获取厂商名称
     * @date 2024/5/18 11:28
     */
    public String getManufacturer() {
        switch (devMode) {
            case ANDROID:
                return "Android";
            case IOS:
                return "ios";
            case WINDOWS:
                return "Windows";
            case LINUX:
                return "Linux";
            case MAC_OS:
                return "Mac OS";
            case HARMONY_OS:
                return "Harmony OS";
            default:
                return "Unknow";
        }
    }

    @Override
    public String toString() {
        return "Device{" +
                "devName='" + devName + '\'' +
                ", devIP='" + devIP + '\'' +
                ", devNetMask='" + devNetMask + '\'' +
                ", devBrotIP='" + devBrotIP + '\'' +
                ", uniqueUUid='" + uniqueUUid + '\'' +
                ", devPort=" + devPort +
                ", devMode=" + devMode +
                ", setTime=" + setTime +
                ", dataVersion=" + dataVersion +
                ", isIPv4=" + isIPv4 +
                ", canRemove=" + canRemove +
                ", interfaceName='" + interfaceName + '\'' +
                ", manufacturer='" + getManufacturer() + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Device device = (Device) obj;
        return device != null && device.getDevIP().equals(getDevIP()) && device.getDevBrotIP().equals(getDevBrotIP()) &&
                device.getDevNetMask().equals(getDevNetMask());
    }

    @Override
    public int hashCode() {
        return (devIP + devNetMask + devPort + devName).hashCode();
    }

    public byte getChargeStatus() {
        return chargeStatus;
    }

    public void setChargeStatus(byte chargeStatus) {
        this.chargeStatus = chargeStatus;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("devName", devName);
        jsonObject.put("devIP", devIP);
        jsonObject.put("devNetMask", devNetMask);
        jsonObject.put("devBrotIP", devBrotIP);
        jsonObject.put("uniqueUUid", uniqueUUid);
        jsonObject.put("devPort", devPort);
        jsonObject.put("devMode", devMode);
        jsonObject.put("dataVersion", dataVersion);
        jsonObject.put("isIPv4", isIPv4);
        jsonObject.put("batteryLevel", batteryLevel);
        jsonObject.put("chargeStatus", chargeStatus);
        return jsonObject;
    }

    public void fromJsonString(JSONObject jsonObject) {
        devName = jsonObject.getString("devName");
        devIP = jsonObject.getString("devIP");
        devNetMask = jsonObject.getString("devNetMask");
        devBrotIP = jsonObject.getString("devBrotIP");
        uniqueUUid = jsonObject.getString("uniqueUUid");
        devPort = jsonObject.getInteger("devPort");
        devMode = jsonObject.getInteger("devMode");
        dataVersion = jsonObject.getInteger("dataVersion");
        batteryLevel = jsonObject.getInteger("batteryLevel");
        chargeStatus = jsonObject.getByte("chargeStatus");
    }

}
