package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.concurrent.Semaphore;

public class MessageStreamContent extends MessageFileContent implements Cloneable {

    private transient InputStream inputStream;
    private transient Semaphore semaphore;

    @Override
    public int getFileType() {
        return FILE_TYPE_STREAM;
    }

    public MessageStreamContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    @NonNull
    @Override
    public MessageStreamContent clone() {
        return (MessageStreamContent) super.clone();
    }
}
