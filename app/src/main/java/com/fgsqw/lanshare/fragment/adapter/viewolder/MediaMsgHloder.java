package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;

public class MediaMsgHloder extends FileMsgHolder {

    public ImageView media;
    public TextView videTime;
    public RelativeLayout mediaInfo;
    public RelativeLayout videTimeLay;

    public MediaMsgHloder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_media_item : R.layout.chat_right_media_item, viewGroup, false));
    }

    public MediaMsgHloder(View itemView) {
        super(itemView);
        media = itemView.findViewById(R.id.chat_media_image);
        videTime = itemView.findViewById(R.id.chat_media_video_time);
        mediaInfo = itemView.findViewById(R.id.chat_media_info_layout);
        videTimeLay = itemView.findViewById(R.id.chat_media_video_time_layout);
    }

}
