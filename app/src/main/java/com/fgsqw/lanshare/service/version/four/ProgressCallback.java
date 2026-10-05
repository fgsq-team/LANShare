package com.fgsqw.lanshare.service.version.four;

import com.fgsqw.lanshare.pojo.message.MessageFileContent;

/**
 * 文件传输进度回调接口
 * <p>用于监听文件传输的进度状态变化</p>
 *
 * @author fgsq
 * @version 1.0
 */
public interface ProgressCallback {
    /**
     * 文件传输开始时调用
     *
     * @param fileTransfer 文件传输对象
     */
    void onStart(FileTransfer fileTransfer);

    /**
     * 文件传输进度更新时调用
     *
     * @param fileTransfer 文件传输对象
     * @param fileItem     当前传输的文件项
     */
    void onProgress(FileTransfer fileTransfer, MessageFileContent fileItem);

    /**
     * 单个文件传输完成时调用
     *
     * @param fileTransfer 文件传输对象
     * @param fileItem     完成传输的文件项
     * @param isSuccess    是否成功
     */
    void onItemFinish(FileTransfer fileTransfer, MessageFileContent fileItem, boolean isSuccess);
}