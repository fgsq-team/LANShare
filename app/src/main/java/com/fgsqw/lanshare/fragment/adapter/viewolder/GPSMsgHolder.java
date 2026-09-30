package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.fgsqw.lanshare.R;

public class GPSMsgHolder extends AbsMsgHolder {

    public TextView tvTitle;

    public GPSMsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_gps_item : R.layout.chat_right_gps_item, viewGroup, false));
    }

    public GPSMsgHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.chat_tv_gps_title);
    }

    @Override
    public void setContentText(String text) {
        tvTitle.setText(text);
    }
}
