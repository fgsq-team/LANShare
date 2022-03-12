package com.fgsqw.lanshare.pojo;


import static java.lang.System.identityHashCode;

/**
 * 弃用
 */
@Deprecated
public class RecordFile extends FileInfo {
    private int progress;
    private Boolean success;
    private String message;
    private mSocket socket;
    private int index;
    private boolean isRecv;

    public boolean isRecv() {
        return isRecv;
    }

    public void setRecv(boolean recv) {
        isRecv = recv;
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

    public Boolean getSuccess() {
        return success;
    }

    public mSocket getSocket() {
        return socket;
    }

    public void setSocket(mSocket socket) {
        this.socket = socket;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean equals(Object obj) {
        return (this == obj);
    }

    public int hashCode() {
        return identityHashCode(this);
    }

}
