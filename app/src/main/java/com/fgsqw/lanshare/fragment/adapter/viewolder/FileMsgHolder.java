package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fgsqw.lanshare.R;

public class FileMsgHolder extends MsgHolder {

    public TextView user;
    public TextView tvSize;
    public TextView stateTv;
    public ProgressBar progressBar;

    public FileMsgHolder(@NonNull View itemView) {
        super(itemView);
        tvSize = itemView.findViewById(R.id.chat_msg_tv_file_size);
        progressBar = itemView.findViewById(R.id.chat_rc_msg_prog);
        user = itemView.findViewById(R.id.chat_item_tv_user);
        stateTv = itemView.findViewById(R.id.chat_rc_msg_canceled);
    }
}
