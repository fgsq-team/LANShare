package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fgsqw.lanshare.R;

public class FileMsgHolder extends MsgHolder {

    public TextView user;
    public TextView tvSize;
    public TextView stateTv;
    public ProgressBar progressBar;
    public ImageView fileTypeIcon;

    public FileMsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_file_item : R.layout.chat_right_file_item, viewGroup, false));
    }

    public FileMsgHolder(View itemView) {
        super(itemView);
        tvSize = itemView.findViewById(R.id.chat_msg_tv_file_size);
        progressBar = itemView.findViewById(R.id.chat_rc_msg_prog);
        user = itemView.findViewById(R.id.chat_item_tv_user);
        stateTv = itemView.findViewById(R.id.chat_rc_msg_canceled);
        fileTypeIcon = itemView.findViewById(R.id.chat_rc_msg_iv_file_type_image);
    }
}
