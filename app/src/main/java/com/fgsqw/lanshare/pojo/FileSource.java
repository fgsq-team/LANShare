package com.fgsqw.lanshare.pojo;

import android.graphics.Bitmap;

public class FileSource extends FileInfo {
    private boolean isPreView;
    private Bitmap preView;
    private boolean isFile;

    public void setPreView(boolean preView) {
        isPreView = preView;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

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
