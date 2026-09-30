package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;

/**
 * @Author: xpw
 */
public class MessageTimeContent extends MessageContent implements Cloneable {
    private String bindId;

    public MessageTimeContent() {
        setTextSelection(false);
    }

    @Override
    public int getViewType() {
        return ChatAdapter.TYPE_TIME_MSG;
    }

    public String getBindId() {
        return bindId;
    }

    public void setBindId(String bindId) {
        this.bindId = bindId;
    }

    @NonNull
    @Override
    public MessageTimeContent clone() {
        return (MessageTimeContent) super.clone();
    }
}
