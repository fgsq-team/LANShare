package com.fgsqw.lanshare.pojo;

import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.fgsqw.lanshare.utils.FileUtil;

import java.io.Serializable;

public class UriFileInfo extends FileInfo implements Serializable {

    private Uri uri;

    public UriFileInfo(Uri uri) {
        this.uri = uri;
        DocumentFile documentFile = FileUtil.getFileRealNameFromUri(uri);
        setName(documentFile.getName());
        setLength(documentFile.length());
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

}
