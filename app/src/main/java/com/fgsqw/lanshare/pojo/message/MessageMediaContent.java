package com.fgsqw.lanshare.pojo.message;

import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

import java.io.Serializable;

public class MessageMediaContent extends MessageFileContent implements Serializable {

    private boolean isVideo;
    private String videoTime;

    private Long mediaId = -1L;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

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
