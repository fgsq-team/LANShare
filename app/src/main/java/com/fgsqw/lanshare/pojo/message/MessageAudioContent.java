package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class MessageAudioContent extends MessageFileContent implements Serializable, Cloneable {
    private long mediaId = -1;
    private String musicTime;

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public long getMediaId() {
        return mediaId;
    }

    public String getMusicTime() {
        return musicTime;
    }

    public void setAudioTime(String musicTime) {
        this.musicTime = musicTime;
    }

    @NonNull
    @Override
    public MessageAudioContent clone() throws CloneNotSupportedException {
        return (MessageAudioContent) super.clone();
    }
}
