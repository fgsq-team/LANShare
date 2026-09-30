package com.fgsqw.lanshare.pojo.file;

import java.io.InputStream;

public class StreamInfo extends FileInfo {
    private transient InputStream inputStream;

    public StreamInfo(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}
