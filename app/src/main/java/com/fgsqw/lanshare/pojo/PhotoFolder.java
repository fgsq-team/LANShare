package com.fgsqw.lanshare.pojo;


import java.util.ArrayList;
import java.util.List;

/**
 * 图片文件夹实体类
 */
public class PhotoFolder {


    private String name;
    private List<PhotoInfo> photoInfos;

    public PhotoFolder(String name) {
        this.name = name;
    }

    public PhotoFolder(String name, List<PhotoInfo> photoInfos) {
        this.name = name;
        this.photoInfos = photoInfos;
    }

    public PhotoFolder(List<PhotoInfo> photoInfos) {
        this.photoInfos = photoInfos;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<PhotoInfo> getImages() {
        return photoInfos;
    }

    public void setImages(List<PhotoInfo> photoInfos) {
        this.photoInfos = photoInfos;
    }


    public void addImage(PhotoInfo photoInfos) {
        if (photoInfos != null && (photoInfos.getPath() != null && photoInfos.getPath().length() > 0)) {//判断对象不为空,判断图片地址不为空
            if (this.photoInfos == null) {               //如果图片列表为空就创建列表对象
                this.photoInfos = new ArrayList<>();
            }
            this.photoInfos.add(photoInfos);
        }
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", photoInfos=" + photoInfos +
                '}';
    }
}
