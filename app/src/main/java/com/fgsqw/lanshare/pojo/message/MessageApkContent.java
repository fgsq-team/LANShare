package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class MessageApkContent extends MessageFileContent implements Serializable, Cloneable {

    @JSONField(serialize = false)
    private byte[] icon;
    private String packageName;
    private String versionName;
    private int versionCode;

    @Override
    public int getFileType() {
        return FILE_TYPE_APK;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    @NonNull
    @Override
    public MessageApkContent clone() {
        return (MessageApkContent) super.clone();
    }

}