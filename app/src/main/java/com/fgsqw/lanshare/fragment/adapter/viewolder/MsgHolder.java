package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fgsqw.lanshare.R;

public class MsgHolder extends RecyclerView.ViewHolder {

    public TextView content;
    public TextView user;
    public ImageView header;

    public MsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_item : R.layout.chat_right_item, viewGroup, false));
    }

    public MsgHolder(View itemView) {
        super(itemView);
        content = itemView.findViewById(R.id.chat_content);
        user = itemView.findViewById(R.id.chat_item_tv_user);
        header = itemView.findViewById(R.id.chat_item_header);
    }

}
