package com.fgsqw.lanshare.pojo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class ApkInfo extends FileInfo {
    private byte[] icon;
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public  byte[] getIcon() {
        return icon;
    }

    public void setIcon( byte[] icon) {
        this.icon = icon;
    }
}
