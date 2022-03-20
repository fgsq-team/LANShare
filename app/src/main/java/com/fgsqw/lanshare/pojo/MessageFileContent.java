package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

public class MessageFileContent extends MessageContent {
    private int progress;
    private String path;
    private Boolean success;
    private long length;
    private mSocket socket;
    private int index;
    private String stateMessage;

    public int getViewType() {
        return isLeft() ? ChatAdabper.TYPE_FILE_MSG_LEFT : ChatAdabper.TYPE_FILE_MSG_RIGHT;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public mSocket getSocket() {
        return socket;
    }

    public void setSocket(mSocket socket) {
        this.socket = socket;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}
