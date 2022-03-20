package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

public class MessageMediaContent extends MessageFileContent {

    private boolean isVideo;
    private String videoTime;


    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public String getVideoTime() {
        return videoTime;
    }

    public void setVideoTime(String videoTime) {
        this.videoTime = videoTime;
    }

    @Override
    public int getViewType() {
        return isLeft() ? ChatAdabper.TYPE_MEDIA_MSG_LEFT : ChatAdabper.TYPE_MEDIA_MSG_RIGHT;
    }

}
