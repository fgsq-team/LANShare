package com.fgsqw.lanshare.pojo;

import java.io.Serializable;

public class Device implements Serializable {
    public static final int UNKNOW = -1;
    public static final int ANDROID = 1;
    public static final int WIN = 2;
    public static final int LINUX = 3;
    public static final int MAC = 4;
    public static final int IOS = 5;

    private String devName;
    private String devIP;
    private String devNetMask; // 子网掩码
    private String devBrotIP;  // 广播IP
    private int devPort;
    private int devMode;       // 设备代号
    private long setTime;
    private int dataVersion; // 通讯协议版本


    private boolean canRemove = true;

    public Device() {
    }

    public Device(String devName, String devIP, int devPort) {
        this.devName = devName;
        this.devIP = devIP;
        this.devPort = devPort;
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

    public String getDeviceName() {
        switch (devMode) {
            case ANDROID:
                return "Android";
            case IOS:
                return "ios";
            case WIN:
                return "Win";
            case LINUX:
                return "Linux";
            case MAC:
                return "Mac";
            default:
                return "unknow";
        }
    }

    @Override
    public String toString() {
        return "Device{" +
                "devName='" + devName + '\'' +
                ", devIP='" + devIP + '\'' +
                ", devNetMask='" + devNetMask + '\'' +
                ", devBrotIP='" + devBrotIP + '\'' +
                ", devPort=" + devPort +
                ", devMode=" + devMode +
                ", setTime=" + setTime +
                '}';
    }
}
