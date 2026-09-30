package com.fgsqw.lanshare.pojo.file;

import com.alibaba.fastjson.annotation.JSONField;

public class ApkInfo extends FileInfo {
    @JSONField(serialize = false)
    private byte[] icon;
    private String packageName;
    private String versionName;
    private int versionCode;

    public ApkInfo() {
        setFile(true);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }


    @Override
    public ApkInfo clone() {
        try {
            return (ApkInfo) super.clone(); // 使用Object的clone方法进行克隆
        } catch (CloneNotSupportedException e) {
            // 实际上，当实现了Cloneable接口时，Object的clone方法不会抛出此异常
            throw new AssertionError();
        }
    }
}
