package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class MessageFolderContent extends MessageFileContent implements Serializable , Cloneable{

    // 文件数量
    private int fileCount;
    // 传输完成数量
    private int completeCount;


    private List<MessageFileContent> children;

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public void setCompleteCount(int completeCount) {
        this.completeCount = completeCount;
    }

    public List<MessageFileContent> getChildren() {
        return children;
    }

    public void setChildren(List<MessageFileContent> children) {
        this.children = children;
    }

    @Override
    public int getFileType() {
        return FILE_TYPE_FOLDER;
    }

    @NonNull
    @Override
    public MessageFolderContent clone() {
        return (MessageFolderContent) super.clone();
    }
}