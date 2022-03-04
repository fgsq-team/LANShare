package com.fgsqw.lanshare.pojo;

import android.graphics.Bitmap;

public class FileSource extends FileInfo {
    boolean isPreView;
    Bitmap preView;


    public Bitmap getPreView() {
        return preView;
    }

    public void setPreView(Bitmap preView) {
        this.preView = preView;
    }

    public boolean isPreView() {
        return isPreView;
    }

    public void setIsPreView(boolean preView) {
        isPreView = preView;
    }
}
