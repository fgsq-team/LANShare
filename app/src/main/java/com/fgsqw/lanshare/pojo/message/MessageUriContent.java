package com.fgsqw.lanshare.pojo.message;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.fgsqw.lanshare.utils.FileUtil;

import java.io.Serializable;

public class MessageUriContent extends MessageFileContent implements Serializable, Cloneable {

    private boolean isFile;
    private transient Uri uri;

    public MessageUriContent(Uri uri) {
        this.uri = uri;
        DocumentFile documentFile = FileUtil.getDocumentFileFromSingleUri(uri);
        setName(documentFile.getName());
        setLength(documentFile.length());
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    @Override
    public int getFileType() {
        return FILE_TYPE_URI;
    }

    @NonNull
    @Override
    public MessageUriContent clone() {
        return (MessageUriContent) super.clone();
    }
}
