package com.fgsqw.lanshare.pojo.network;

import com.fgsqw.lanshare.pojo.file.PhotoFolder;
import com.fgsqw.lanshare.pojo.file.MediaInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaResult {
    private List<PhotoFolder> mFolders;

    private List<MediaInfo> allMedia;
    private Map<Integer, MediaInfo> allMediaMap;

    Map<Long, MediaInfo> mediaInfoMap;


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

    public void setAllMediaMap(Map<Integer, MediaInfo> allMediaMap) {
        this.allMediaMap = allMediaMap;
    }

    public Map<Integer, MediaInfo> getAllMediaMap() {
        return allMediaMap;
    }

    public Map<Long, MediaInfo> getMediaInfoMap() {
        return mediaInfoMap;
    }

    public void setMediaInfoMap(Map<Long, MediaInfo> mediaInfoMap) {
        this.mediaInfoMap = mediaInfoMap;
    }
}
