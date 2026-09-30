package com.fgsqw.lanshare.pojo.file;

import java.io.Serializable;

public class MusicInfo extends FileInfo implements Serializable {
    private String musicTime;

    public String getMusicTime() {
        return musicTime;
    }

    public void setMusicTime(String musicTime) {
        this.musicTime = musicTime;
    }
}
