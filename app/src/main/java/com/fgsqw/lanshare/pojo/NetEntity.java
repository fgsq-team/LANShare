package com.fgsqw.lanshare.pojo;

public class NetEntity {

    // 接口名
    String name;
    // 接口IP
    String ip;
    // 接口广播ip
    String brodIp;
    // 子网掩码
    String mask;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBrodIp() {
        return brodIp;
    }

    public void setBrodIp(String brodIp) {
        this.brodIp = brodIp;
    }

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    @Override
    public String toString() {
        return "NetEntity{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", brodIp='" + brodIp + '\'' +
                ", mask='" + mask + '\'' +
                '}';
    }
}
