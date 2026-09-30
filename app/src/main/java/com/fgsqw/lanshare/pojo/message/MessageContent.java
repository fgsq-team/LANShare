package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;

import java.io.Serializable;
import java.util.Date;


public class MessageContent implements Serializable, Cloneable {

    public static final int IN = 0x2;                   // 进行中
    public static final int SUCCESS = 0x4;              // 成功
    public static final int ERROR = 0x8;                // 失败
    public static final int FILE_NOT_EXIST = 0x10;      // 文件不存在

    // 主键
    private String id;
    // 消息内容
    private String content;
    // 消息是否在左边
    private boolean isLeft;
    // 设备类型
    private int devMode = 0;
    // 用户名
    private String userName;
    // 发送给哪个用户名
    private String toUser;
    // 创建时间
    private Date createTime;
    // 消息状态
    private int status = IN;
    private int dataVersion = 0;
    private boolean checked;
    // 文本选中
    private boolean textSelection = true;

    public MessageContent() {
        createTime = new Date();
    }

    public void setDevMode(int devMode) {
        this.devMode = devMode;
    }

    public int getDevMode() {
        return devMode;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getViewType() {
        return isLeft ? ChatAdapter.TYPE_MSG_LEFT : ChatAdapter.TYPE_MSG_RIGHT;
    }


    public void setContent(String content) {
        this.content = content;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isTextSelection() {
        return textSelection;
    }

    public void setTextSelection(boolean textSelection) {
        this.textSelection = textSelection;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MessageContent) {
            return getId().equals(((MessageContent) obj).getId());
        }
        return false;
    }


    public void addStatus(int status) {
        this.status = this.status | status;
    }


    // 判断消息状态
    public boolean existStatus(int... status) {
        if (status == null) {
            return false;
        }
        for (int i : status) {
            if ((this.status & i) > 0) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public MessageContent clone() {
        try {
            return (MessageContent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }


}
