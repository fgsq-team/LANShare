package com.fgsqw.lanshare.pojo;

public class StorageInfo {
    // 名称
    private String name;
    // 路径
    private String path;

    private String uuid;

    // 可移除的
    private Boolean isRemovable;
    private Boolean isEmulated;

    // 总容量
    private Long totalSize;
    // 可用容量
    private Long avaibleSize;

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public Boolean getRemovable() {
        return isRemovable;
    }

    public void setRemovable(Boolean removable) {
        isRemovable = removable;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getAvaibleSize() {
        return avaibleSize;
    }

    public void setAvaibleSize(Long avaibleSize) {
        this.avaibleSize = avaibleSize;
    }

    public Boolean getEmulated() {
        return isEmulated;
    }

    public void setEmulated(Boolean emulated) {
        isEmulated = emulated;
    }
}
