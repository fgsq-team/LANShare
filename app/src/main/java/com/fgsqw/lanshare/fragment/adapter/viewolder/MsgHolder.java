package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.content.Context;
import android.widget.CheckBox;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;

public class MsgHolder extends AbsMsgHolder {

    public TextView content;
    public TextView user;
    public ImageView header;
    public CheckBox checkBox;

    public MsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_item : R.layout.chat_right_item, viewGroup, false));
    }

    public MsgHolder(View itemView) {
        super(itemView);
        content = itemView.findViewById(R.id.chat_content);
        user = itemView.findViewById(R.id.chat_item_tv_user);
        header = itemView.findViewById(R.id.chat_item_header);
        checkBox = itemView.findViewById(R.id.chat_item_check_box);
    }

    public void setTextIsSelectable(boolean selectable) {
        content.setTextIsSelectable(selectable);
    }

    public void setContentText(String text) {
        content.setText(text);
    }

    public void setHeaderRes(Context context, RequestOptions options, int h) {
        // 头像
        Glide.with(context).load(h)
                .apply(options)
                .into(header);
    }

    public void setUserText(String text) {
        user.setText(text);
    }

    public void setCheckVisibility(boolean check) {
        checkBox.setVisibility(check ? View.VISIBLE : View.GONE);
    }

    public void setCheck(boolean check) {
        checkBox.setChecked(check);
    }


}
