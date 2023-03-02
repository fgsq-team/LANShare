package com.fgsqw.lanshare.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaResult {
    private List<PhotoFolder> mFolders;

    private List<MediaInfo> allMedia;
    private Map<Integer, MediaInfo> allMediaMap;


    public List<PhotoFolder> getmFolders() {
        return mFolders;
    }

    public void setmFolders(List<PhotoFolder> mFolders) {
        this.mFolders = mFolders;
    }

    public List<MediaInfo> getAllMedia() {
        return new ArrayList<>(allMediaMap.values());
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
}
