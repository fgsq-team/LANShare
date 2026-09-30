package com.fgsqw.lanshare.pojo.file;

public class DownloadInfo extends FileInfo {
    private boolean downloaded;
    private int days;

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
}
