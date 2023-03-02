package com.fgsqw.lanshare.pojo;

import java.io.Serializable;

public class MediaInfo extends FileInfo implements Serializable {

    boolean isGif;
    boolean isVideo;
    String videoTime;
    int index;

    public String getVideoTime() {
        return videoTime;
    }

    public void setVideoTime(String videoTime) {
        this.videoTime = videoTime;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public void setGif(boolean gif) {
        isGif = gif;
    }

    public boolean isGif() {
        return isGif;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
