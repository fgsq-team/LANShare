package com.fgsqw.lanshare.service.version.five;

import com.fgsqw.lanshare.service.version.four.FileTransfer;

import java.util.HashMap;
import java.util.Map;

/**
 * V5 文件传输对象
 * <p>在 V4 {@link FileTransfer} 基础上增量携带断点续传协商信息:</p>
 * <ul>
 *     <li>{@code resumeEnabled}:本次传输是否启用续传(双端均为 V5)</li>
 *     <li>{@code resumeMap}:接收端回传的每个 fileId 已完成字节偏移</li>
 * </ul>
 *
 * @author fgsq
 * @version 5.0
 */
public class FileTransferV5 extends FileTransfer {

    /** 是否启用断点续传 */
    private boolean resumeEnabled;

    /** fileId -> 已完成字节偏移 */
    private Map<String, Long> resumeMap = new HashMap<>();

    public boolean isResumeEnabled() {
        return resumeEnabled;
    }

    public void setResumeEnabled(boolean resumeEnabled) {
        this.resumeEnabled = resumeEnabled;
    }

    public Map<String, Long> getResumeMap() {
        return resumeMap;
    }

    public void setResumeMap(Map<String, Long> resumeMap) {
        this.resumeMap = resumeMap == null ? new HashMap<>() : resumeMap;
    }

    /**
     * 获取指定 fileId 的续传偏移量。
     *
     * @param fileId 文件稳定 id
     * @return 已完成字节数,无记录返回 0
     */
    public long getResumeOffset(String fileId) {
        if (fileId == null) {
            return 0;
        }
        Long offset = resumeMap.get(fileId);
        return offset == null ? 0 : offset;
    }
}
