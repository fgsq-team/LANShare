package com.fgsqw.lanshare.pojo.network;

import com.fgsqw.lanshare.pojo.file.PhotoFolder;
import com.fgsqw.lanshare.pojo.message.MessageMediaContent;

import java.util.List;
import java.util.Map;

public class MediaResult {
    private List<PhotoFolder> mFolders;

    private List<MessageMediaContent> allMedia;
    private Map<Integer, MessageMediaContent> allMediaMap;

    Map<Long, MessageMediaContent> mediaInfoMap;


    public List<PhotoFolder> getmFolders() {
        return mFolders;
    }

    public void setmFolders(List<PhotoFolder> mFolders) {
        this.mFolders = mFolders;
    }

    public List<MessageMediaContent> getAllMedia() {
        return allMedia;
    }

    public void setAllMedia(List<MessageMediaContent> allMedia) {
        this.allMedia = allMedia;
    }

    public void setAllMediaMap(Map<Integer, MessageMediaContent> allMediaMap) {
        this.allMediaMap = allMediaMap;
    }

    public Map<Integer, MessageMediaContent> getAllMediaMap() {
        return allMediaMap;
    }

    public Map<Long, MessageMediaContent> getMediaInfoMap() {
        return mediaInfoMap;
    }

    public void setMediaInfoMap(Map<Long, MessageMediaContent> mediaInfoMap) {
        this.mediaInfoMap = mediaInfoMap;
    }
}
