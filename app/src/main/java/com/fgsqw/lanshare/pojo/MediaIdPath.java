package com.fgsqw.lanshare.pojo;


import java.util.Date;

public class MediaIdPath {
    private long id;
    private String name;
    private String path;
    private Date creationTime;
    private boolean isReceived;

    public MediaIdPath(long id, String name, String path, Date creationTime, boolean isReceived) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.creationTime = creationTime;
        this.isReceived = isReceived;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean received) {
        isReceived = received;
    }

    @Override
    public String toString() {
        return "MediaIdPath{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", creationTime=" + creationTime +
                ", isReceived=" + isReceived +
                '}';
    }
}
