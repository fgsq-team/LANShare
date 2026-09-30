package com.fgsqw.lanshare.pojo.file;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;
import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.Objects;

public class FileInfo implements Serializable {
    private String uuid;
    private String name;
    private String path;
    private long length;
    private long time;

    private boolean isPreView;
    long mediaId = -1;

    @JSONField(serialize = false) // 在序列化时忽略该字段
    private Bitmap preView;
    private boolean isFile;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setPreView(boolean preView) {
        isPreView = preView;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public Bitmap getPreView() {
        return preView;
    }

    public void setPreView(Bitmap preView) {
        this.preView = preView;
    }

    public boolean isPreView() {
        return isPreView;
    }

    public void setIsPreView(boolean preView) {
        isPreView = preView;
    }


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public FileInfo() {
    }

    public FileInfo(String name, String path, long length) {
        this.name = name;
        this.path = path;
        this.length = length;
    }

    public FileInfo(String name, String path, long length, long time) {
        this.name = name;
        this.path = path;
        this.length = length;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    @SuppressWarnings("all")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return Objects.equals(name, fileInfo.name) && Objects.equals(path, fileInfo.path);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }

    public int compareTo(FileInfo fileInfo) {
        return path.toLowerCase().compareTo(fileInfo.path.toLowerCase());
    }
}
