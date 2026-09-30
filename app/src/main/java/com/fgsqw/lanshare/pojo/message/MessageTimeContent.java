package com.fgsqw.lanshare.pojo.message;

import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

/**
 * @Author: xpw
 */
public class MessageTimeContent extends MessageContent {
    private String bindId;

    public MessageTimeContent() {
        setTextSelection(false);
    }

    @Override
    public int getViewType() {
        return ChatAdabper.TYPE_TIME_MSG;
    }

    public String getBindId() {
        return bindId;
    }

    public void setBindId(String bindId) {
        this.bindId = bindId;
    }
}
