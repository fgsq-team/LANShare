package com.fgsqw.lanshare.pojo;

import java.util.List;

public class MessageFolderContent extends MessageFileContent {

    // 文件数量
    private int fileCount;
    // 传输完成数量
    private int completeCount;

    private String basePath;

    private List<FileInfo> fileInfoList;

    @Override
    public String getContent() {
        return fileCount + "/" + completeCount;
    }

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

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }
}