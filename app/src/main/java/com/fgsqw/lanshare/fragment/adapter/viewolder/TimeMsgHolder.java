package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;

public class TimeMsgHolder extends AbsMsgHolder {

    public TextView tvTime;

    public TimeMsgHolder(LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(R.layout.chat_time_item, viewGroup, false));
    }

    public TimeMsgHolder(View itemView) {
        super(itemView);
        tvTime = itemView.findViewById(R.id.chat_tv_time);
    }

}
