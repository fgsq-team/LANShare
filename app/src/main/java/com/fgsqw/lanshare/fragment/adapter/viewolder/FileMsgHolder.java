package com.fgsqw.lanshare.fragment.adapter.viewolder;

import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.pojo.message.MessageFileContent;
import com.fgsqw.lanshare.utils.BubbleDrawable;
import com.fgsqw.lanshare.utils.FileUtil;

public class FileMsgHolder extends MsgHolder {

    public TextView user;
    public TextView tvSize;
    public TextView tvSpeed;
    public TextView stateTv;
    public ProgressBar progressBar;
    public ImageView fileTypeIcon;

    public FileMsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        this(mInflater.inflate(isLeft ? R.layout.chat_left_file_item : R.layout.chat_right_file_item, viewGroup, false), isLeft);
    }

    public FileMsgHolder(View itemView, boolean isLeft) {
        super(itemView);
        tvSize = itemView.findViewById(R.id.chat_msg_tv_file_size);
        tvSpeed = itemView.findViewById(R.id.chat_msg_tv_speed);
        progressBar = itemView.findViewById(R.id.chat_rc_msg_prog);
        user = itemView.findViewById(R.id.chat_item_tv_user);
        stateTv = itemView.findViewById(R.id.chat_rc_msg_canceled);
        fileTypeIcon = itemView.findViewById(R.id.chat_rc_msg_iv_file_type_image);
        initBubble(itemView, isLeft);
    }

    /**
     * 气泡：圆角+尖角背景、elevation 阴影，并放开父容器裁剪让阴影完整显示
     */
    private void initBubble(View itemView, boolean isLeft) {
        float d = itemView.getResources().getDisplayMetrics().density;
        View bubble = itemView.findViewById(R.id.rc_message);
        if (bubble != null) {
            BubbleDrawable bg = new BubbleDrawable(
                    isLeft ? 0xFFFFFFFF : 0xFFCCEAFF, isLeft, 12 * d, 5 * d, 10 * d);
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                bubble.setBackground(bg);
            } else {
                bubble.setBackgroundDrawable(bg);
            }
        }
        if (itemView instanceof ViewGroup) {
            ((ViewGroup) itemView).setClipToPadding(false);
        }
        View mid = itemView.findViewById(R.id.chat_bubble_container);
        if (mid instanceof ViewGroup) {
            ((ViewGroup) mid).setClipChildren(false);
        }
    }

    /**
     * 传输过程中刷新发送速度：仅右侧发送项、传输中且已算出速度时显示
     */
    public void updateSpeed(MessageFileContent content) {
        if (tvSpeed == null || content == null) {
            return;
        }
        long speed = content.getSpeed();
        boolean show = content.existStatus(MessageContent.IN) && speed > 0;
        android.util.Log.d("SPD", "updateSpeed show=" + show + " sp=" + speed
                + " left=" + content.isLeft() + " IN=" + content.existStatus(MessageContent.IN)
                + " p=" + content.getProgress());
        if (show) {
            tvSpeed.setVisibility(View.VISIBLE);
            tvSpeed.setText(FileUtil.formatSpeed(speed));
        } else {
            tvSpeed.setVisibility(View.GONE);
        }
    }
}
