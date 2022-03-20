package com.fgsqw.lanshare.pojo;


import java.util.ArrayList;
import java.util.List;

/**
 * 图片文件夹实体类
 */
public class PhotoFolder {


    private String name;
    private List<MediaInfo> mediaInfos;

    public PhotoFolder(String name) {
        this.name = name;
    }

    public PhotoFolder(String name, List<MediaInfo> mediaInfos) {
        this.name = name;
        this.mediaInfos = mediaInfos;
    }

    public PhotoFolder(List<MediaInfo> mediaInfos) {
        this.mediaInfos = mediaInfos;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MediaInfo> getImages() {
        return mediaInfos;
    }

    public void setImages(List<MediaInfo> mediaInfos) {
        this.mediaInfos = mediaInfos;
    }


    public void addImage(MediaInfo mediaInfos) {
        if (mediaInfos != null && (mediaInfos.getPath() != null && mediaInfos.getPath().length() > 0)) {//判断对象不为空,判断图片地址不为空
            if (this.mediaInfos == null) {               //如果图片列表为空就创建列表对象
                this.mediaInfos = new ArrayList<>();
            }
            this.mediaInfos.add(mediaInfos);
        }
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", photoInfos=" + mediaInfos +
                '}';
    }
}
