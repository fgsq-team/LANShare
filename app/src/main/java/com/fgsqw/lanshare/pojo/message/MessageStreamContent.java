package com.fgsqw.lanshare.pojo.message;

import java.io.InputStream;

public class MessageStreamContent extends MessageFileContent {

    private transient InputStream inputStream;

    public MessageStreamContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

}
