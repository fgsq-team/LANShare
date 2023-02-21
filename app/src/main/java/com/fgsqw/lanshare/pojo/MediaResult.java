package com.fgsqw.lanshare.pojo;

import java.util.List;

public class MediaResult {
    private List<PhotoFolder> mFolders;

    private List<MediaInfo> allMedia;

    public List<PhotoFolder> getmFolders() {
        return mFolders;
    }

    public void setmFolders(List<PhotoFolder> mFolders) {
        this.mFolders = mFolders;
    }

    public List<MediaInfo> getAllMedia() {
        return allMedia;
    }

    public void setAllMedia(List<MediaInfo> allMedia) {
        this.allMedia = allMedia;
    }
}
