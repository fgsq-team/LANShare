package com.fgsqw.lanshare.pojo;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Objects;

public class FileInfo {
    private String name;
    private String path;
    private long length;
    private long time;

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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
}
