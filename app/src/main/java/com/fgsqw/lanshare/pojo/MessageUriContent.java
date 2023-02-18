package com.fgsqw.lanshare.pojo;

import android.net.Uri;

import java.io.Serializable;

public class MessageUriContent extends MessageFileContent implements Serializable {

    private transient Uri uri;

    public MessageUriContent(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
