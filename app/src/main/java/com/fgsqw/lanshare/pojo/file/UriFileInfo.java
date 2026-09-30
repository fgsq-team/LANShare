package com.fgsqw.lanshare.pojo.file;

import android.net.Uri;


import androidx.documentfile.provider.DocumentFile;

import com.fgsqw.lanshare.utils.FileUtil;

import java.io.Serializable;

public class UriFileInfo extends FileInfo implements Serializable {

    private transient Uri uri;

    public UriFileInfo(Uri uri) {
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

}
