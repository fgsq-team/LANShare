package com.fgsqw.lanshare.pojo;

import java.io.Serializable;

public class MediaInfo extends FileInfo implements Serializable {

    boolean isGif;
    boolean isVideo;
    String videoTime;

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
}
