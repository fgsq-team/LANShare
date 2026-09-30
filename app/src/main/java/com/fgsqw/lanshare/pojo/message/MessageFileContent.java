package com.fgsqw.lanshare.pojo.message;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.annotation.JSONField;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;
import com.fgsqw.lanshare.service.CustomDataOutputStream;
import com.fgsqw.lanshare.toast.T;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class MessageFileContent extends MessageContent implements Serializable, Cloneable {

    public static final int FILE_TYPE_IMAGE = 0x1;
    public static final int FILE_TYPE_VIDEO = 0x2;
    public static final int FILE_TYPE_AUDIO = 0x3;
    public static final int FILE_TYPE_APK = 0x4;
    public static final int FILE_TYPE_FILE = 0x5;
    public static final int FILE_TYPE_FOLDER = 0x6;
    public static final int FILE_TYPE_DOWNLOAD_INFO = 0x7;
    public static final int FILE_TYPE_STREAM = 0x8;
    public static final int FILE_TYPE_URI = 0x9;

    private String fileId;
    private int fileType = FILE_TYPE_FILE;
    private int progress;
    private String path;
    private long length;
    private long completedSize;
    private int index;
    private String stateMessage;
    private long time;
    @JSONField(serialize = false)
    private boolean isTransfer = true;
    @JSONField(serialize = false)
    private CustomDataOutputStream outputStream;
    private boolean isGif;
    @JSONField(serialize = false)
    private Bitmap previewBitmap;
    @JSONField(serialize = false)
    private boolean preView;

    public MessageFileContent() {
        setTextSelection(false);
    }

    // 发送某个文件取消传输指令
    public void cancelFileTransfer() throws IOException {
        isTransfer = false;
        if (outputStream == null) {
            return;
        }
        outputStream.writeString(fileId);
        T.s(R.string.cancel_file_transfer_success);
    }


    public boolean isTransfer() {
        return isTransfer;
    }

    public void setTransfer(boolean transfer) {
        this.isTransfer = transfer;
    }

    public int getViewType() {
        return isLeft() ? ChatAdapter.TYPE_FILE_MSG_LEFT : ChatAdapter.TYPE_FILE_MSG_RIGHT;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
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

    public long getCompletedSize() {
        return completedSize;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public void setName(String name) {
        setContent(name);
    }

    public String getName() {
        return getContent();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setGif(boolean isGif) {
        this.isGif = isGif;
    }

    public boolean isGif() {
        return isGif;
    }

    public void setPreviewBitmap(Bitmap previewBitmap) {
        this.previewBitmap = previewBitmap;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public void setIsPreView(boolean preView) {
        this.preView = preView;
    }

    public boolean isPreView() {
        return preView;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public CustomDataOutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(CustomDataOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setPreView(boolean preView) {
        this.preView = preView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageFileContent fileInfo = (MessageFileContent) o;
        return Objects.equals(getName(), fileInfo.getName()) && Objects.equals(path, fileInfo.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), path);
    }

    public int compareTo(MessageFileContent fileInfo) {
        return path.toLowerCase().compareTo(fileInfo.path.toLowerCase());
    }

    @NonNull
    @Override
    public MessageFileContent clone() {
        return (MessageFileContent) super.clone();
    }
}
