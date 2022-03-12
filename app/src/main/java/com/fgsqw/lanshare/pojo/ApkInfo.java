package com.fgsqw.lanshare.pojo;

import android.graphics.drawable.Drawable;

public class ApkInfo extends FileInfo {
    private Drawable icon;
    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }
}
