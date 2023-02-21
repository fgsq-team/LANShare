package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

import java.io.Serializable;
import java.util.Date;


public class MessageContent implements Serializable {

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
    private int header = 0;
    // 用户名
    private String userName;
    // 发送给哪个用户名
    private String toUser;
    // 创建时间
    private Date createTime;
    // 消息状态
    private int status = IN;

    public MessageContent() {
        createTime = new Date();
    }

    public int getHeader() {
        if (header == 0) {
            return R.drawable.ic_phone;
        }
        return header;
    }

    public void setHeader(int header) {
        this.header = header;
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
        return isLeft ? ChatAdabper.TYPE_MSG_LEFT : ChatAdabper.TYPE_MSG_RIGHT;
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


}
