package com.fgsqw.lanshare.service.version.four;

import com.fgsqw.lanshare.pojo.message.MessageFileContent;

// 进度回调
public interface ProgressCallback {
    void onStart(FileTransfer fileTransfer);

    void onProgress(FileTransfer fileTransfer, MessageFileContent fileItem);

    void onItemFinish(FileTransfer fileTransfer, MessageFileContent fileItem, boolean isSuccess);
}