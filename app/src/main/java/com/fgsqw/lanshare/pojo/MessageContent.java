package com.fgsqw.lanshare.pojo;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

public class MessageContent {
    private String content;
    private boolean isLeft;
    private int header = 0;
    private String userName;
    private String toUser;

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
        if (isLeft) {
            return ChatAdabper.ITEM_TYPE.TYPE_LEFT_MSG.ordinal();
        } else {
            return ChatAdabper.ITEM_TYPE.TYPE_RIGHT_MSG.ordinal();
        }
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
}
