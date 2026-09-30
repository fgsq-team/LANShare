package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MessageDownloadInfoContent extends MessageFileContent implements Serializable, Cloneable {
    private boolean downloaded;
    private int days;

    @Override
    public int getFileType() {
        return FILE_TYPE_DOWNLOAD_INFO;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    @NonNull
    @Override
    public MessageDownloadInfoContent clone() {
        return (MessageDownloadInfoContent) super.clone();
    }
}
