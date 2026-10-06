package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;

import java.io.Serializable;

public class MessageMediaContent extends MessageFileContent implements Serializable, Cloneable {

    private boolean isVideo;
    private String videoTime;
    private boolean isLivePhoto;
    private String liveVideoPath;

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

    public boolean isLivePhoto() {
        return isLivePhoto;
    }

    public void setLivePhoto(boolean livePhoto) {
        isLivePhoto = livePhoto;
    }

    public String getLiveVideoPath() {
        return liveVideoPath;
    }

    public void setLiveVideoPath(String liveVideoPath) {
        this.liveVideoPath = liveVideoPath;
    }

    @Override
    public int getViewType() {
        return isLeft() ? ChatAdapter.TYPE_MEDIA_MSG_LEFT : ChatAdapter.TYPE_MEDIA_MSG_RIGHT;
    }

    @Override
    public int getFileType() {
        return isVideo ? FILE_TYPE_VIDEO : FILE_TYPE_IMAGE;
    }

    @NonNull
    @Override
    public MessageMediaContent clone() {
        return (MessageMediaContent) super.clone();
    }
}
