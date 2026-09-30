package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.viewolder.FileMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.GPSMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.MediaMsgHloder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.MsgHolder;
import com.fgsqw.lanshare.fragment.FragmentChat;
import com.fgsqw.lanshare.fragment.adapter.viewolder.TimeMsgHolder;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.*;
import com.fgsqw.lanshare.utils.DateUtils;
import com.fgsqw.lanshare.utils.FileUtil;

import java.util.Objects;

public class ChatAdabper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_TIME_MSG = -1;
    public static final int TYPE_MSG_LEFT = 1;
    public static final int TYPE_MSG_RIGHT = 2;
    public static final int TYPE_FILE_MSG_LEFT = 3;
    public static final int TYPE_FILE_MSG_RIGHT = 4;
    public static final int TYPE_MEDIA_MSG_LEFT = 5;
    public static final int TYPE_MEDIA_MSG_RIGHT = 6;
    public static final int TYPE_GPS_MSG_LEFT = 7;
    public static final int TYPE_GPS_MSG_RIGHT = 8;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final RequestOptions options;
    private final FragmentChat fragmentChat;
    private final int stateTvColor;
    private boolean checkMode = false;
    private OnItemLongClickListener mLongListener;
    private OnItemClickListener mListener;
    private OnCheckedChangeListener mCheckedChangeListener;

    public ChatAdabper(FragmentChat fragmentChat) {
        mContext = fragmentChat.getContext();
        stateTvColor = Objects.requireNonNull(mContext).getResources().getColor(R.color.item_text);
        this.fragmentChat = fragmentChat;
        mInflater = LayoutInflater.from(mContext);
        options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        MsgHolder msgHolder;
        if (viewType == TYPE_TIME_MSG) {
            msgHolder = new TimeMsgHolder(mInflater, viewGroup);
        } else if (viewType == TYPE_MSG_LEFT) {
            msgHolder = new MsgHolder(true, mInflater, viewGroup);
        } else if (viewType == TYPE_MSG_RIGHT) {
            msgHolder = new MsgHolder(false, mInflater, viewGroup);
        } else if (viewType == TYPE_FILE_MSG_LEFT) {
            msgHolder = new FileMsgHolder(true, mInflater, viewGroup);
        } else if (viewType == TYPE_FILE_MSG_RIGHT) {
            msgHolder = new FileMsgHolder(false, mInflater, viewGroup);
        } else if (viewType == TYPE_MEDIA_MSG_LEFT) {
            msgHolder = new MediaMsgHloder(true, mInflater, viewGroup);
        } else if (viewType == TYPE_MEDIA_MSG_RIGHT) {
            msgHolder = new MediaMsgHloder(false, mInflater, viewGroup);
        } else if (viewType == TYPE_GPS_MSG_LEFT) {
            msgHolder = new GPSMsgHolder(true, mInflater, viewGroup);
        } else/* if (viewType == TYPE_GPS_MSG_RIGHT)*/ {
            msgHolder = new GPSMsgHolder(false, mInflater, viewGroup);
        }
        return msgHolder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        MessageContent messageContent = fragmentChat.getMessageContents().get(position);
        MsgHolder msgHolder = (MsgHolder) viewHolder;
        msgHolder.setCheckVisibility(checkMode);
        msgHolder.setCheck(messageContent.isChecked());
        // 头像
        if (messageContent.getDevMode() == Device.WINDOWS) {
            msgHolder.setHeaderRes(mContext, options, R.drawable.ic_pc);
        } else {
            msgHolder.setHeaderRes(mContext, options, R.drawable.ic_phone);
        }
        // 设置文本可自由复制
        msgHolder.setTextIsSelectable(messageContent.isTextSelection());
        // 设置消息
        msgHolder.setContentText(messageContent.getContent());
        // 设置用户名
        if (messageContent.isLeft()) {
            msgHolder.setUserText(messageContent.getUserName());
        } else {
            msgHolder.setUserText(messageContent.getToUser() + " ← " + messageContent.getUserName());
        }
        int itemViewType = messageContent.getViewType();
        // 判断为文件
        if (itemViewType == TYPE_FILE_MSG_LEFT || itemViewType == TYPE_FILE_MSG_RIGHT) {
            FileMsgHolder fileMsgHolder = (FileMsgHolder) viewHolder;
            MessageFileContent messageFileContent = (MessageFileContent) messageContent;
            fileMsgHolder.progressBar.setProgress(messageFileContent.getProgress());
            fileMsgHolder.content.setText(messageFileContent.getContent());
            fileMsgHolder.tvSize.setText(FileUtil.computeSize(messageFileContent.getLength()));
            if (messageFileContent instanceof MessageFolderContent) {
                Glide.with(mContext).load(R.drawable.rc_file_blue_icon)
                        .apply(options)
                        .into(fileMsgHolder.fileTypeIcon);
            } else {
                Glide.with(mContext).load(R.drawable.rc_file_icon_file)
                        .apply(options)
                        .into(fileMsgHolder.fileTypeIcon);
            }
            boolean status = !messageFileContent.existStatus(MessageContent.IN);
            fileMsgHolder.progressBar.setVisibility(status ? View.GONE : View.VISIBLE);
            fileMsgHolder.stateTv.setVisibility(status ? View.VISIBLE : View.GONE);
            if (status) {
                fileMsgHolder.stateTv.setText(messageFileContent.getStateMessage());
                if (messageFileContent.existStatus(MessageContent.SUCCESS)) {
                    fileMsgHolder.stateTv.setTextColor(stateTvColor);
                } else if (messageFileContent.existStatus(MessageContent.ERROR)) {
                    fileMsgHolder.stateTv.setTextColor(Color.RED);
                }
            }
            fileMsgHolder.updateSpeed(messageFileContent);
            // 判断为媒体文件
        } else if (itemViewType == TYPE_MEDIA_MSG_LEFT || itemViewType == TYPE_MEDIA_MSG_RIGHT) {
            MediaMsgHloder mediaMsgHloder = (MediaMsgHloder) viewHolder;
            MessageMediaContent mediaContent = (MessageMediaContent) messageContent;
            mediaMsgHloder.tvSize.setText(FileUtil.computeSize(mediaContent.getLength()));
            mediaMsgHloder.progressBar.setProgress(mediaContent.getProgress());
            mediaMsgHloder.videTimeLay.setVisibility(mediaContent.isVideo() ? View.VISIBLE : View.GONE);
            // 判断是否为视频
            if (mediaContent.isVideo()) {
                mediaMsgHloder.videTime.setText(mediaContent.getVideoTime());
            }
            // 判断文件传输是否已完成
            if (mediaContent.existStatus(MessageContent.SUCCESS, MessageContent.ERROR)) {
                mediaMsgHloder.progressBar.setVisibility(View.GONE);
                mediaMsgHloder.stateTv.setVisibility(View.VISIBLE);
                boolean statusSuccess = mediaContent.existStatus(MessageContent.SUCCESS);
                mediaMsgHloder.mediaInfo.setVisibility(statusSuccess ? View.GONE : View.VISIBLE);
                mediaMsgHloder.stateTv.setTextColor(statusSuccess ? stateTvColor : Color.RED);
                mediaMsgHloder.stateTv.setText(mediaContent.getStateMessage());
                Glide.with(mContext).load(statusSuccess ? mediaContent.getPath() : ((Drawable) null))
                        .apply(options)
                        .into(mediaMsgHloder.media);
            } else {
                mediaMsgHloder.progressBar.setVisibility(View.VISIBLE);
                mediaMsgHloder.stateTv.setVisibility(View.GONE);
                mediaMsgHloder.mediaInfo.setVisibility(View.VISIBLE);
                Glide.with(mContext).load(R.drawable.image_background)
                        .apply(options)
                        .into(mediaMsgHloder.media);
            }
        } else if (itemViewType == TYPE_GPS_MSG_LEFT || itemViewType == TYPE_GPS_MSG_RIGHT) {
            GPSMsgHolder gpsMsgHolder = (GPSMsgHolder) viewHolder;
            MessageGPSContent gpsContent = (MessageGPSContent) messageContent;
            gpsMsgHolder.bindGPS(gpsContent);
        } else if (itemViewType == TYPE_TIME_MSG) {
            TimeMsgHolder timeMsgHolder = (TimeMsgHolder) viewHolder;
            MessageTimeContent timeContent = (MessageTimeContent) messageContent;
            long time = timeContent.getCreateTime().getTime();
            boolean today = DateUtils.isToday(time);
            if (today) {
                String timeOfDay = DateUtils.getTimeOfDay(time);
                timeMsgHolder.tvTime.setText(timeOfDay + DateUtils.formatDate(timeContent.getCreateTime(), "HH:mm"));
            } else if (DateUtils.isYesterday(time)) {
                timeMsgHolder.tvTime.setText("昨天" + DateUtils.formatDate(timeContent.getCreateTime(), "HH:mm"));
            } else if (DateUtils.isCurrentMonth(time) || DateUtils.isCurrentYear(time)) {
                timeMsgHolder.tvTime.setText(DateUtils.formatDate(timeContent.getCreateTime(), "MM月dd日 HH:mm"));
            } else {
                timeMsgHolder.tvTime.setText(DateUtils.formatDate(timeContent.getCreateTime(), "yyyy年MM月dd日 HH:mm"));
            }
        }
        viewHolder.itemView.setOnClickListener((v) -> {
            if (mListener != null)
                mListener.onItemClick(messageContent, position);
        });
        viewHolder.itemView.setOnLongClickListener((v) -> {
            if (mLongListener != null)
                return mLongListener.onItemLongClick(messageContent, viewHolder.itemView, position);
            return false;
        });
        if (msgHolder.checkBox != null) {
            msgHolder.checkBox.setOnClickListener(v -> {
                messageContent.setChecked(msgHolder.checkBox.isChecked());
                if (mCheckedChangeListener != null) {
                    mCheckedChangeListener.onItemCheckedChanged(msgHolder.checkBox.isChecked(), messageContent, position);
                }
            });
        } else {
            messageContent.setChecked(false);
        }
    }

    public boolean isCheckMode() {
        return checkMode;
    }

    public void setCheckMode(boolean checkMode) {
        this.checkMode = checkMode;
        refresh();
    }

    public int getDataPosition(MessageContent messageContent) {
        return fragmentChat.getMessageContents().indexOf(messageContent);
    }


    @Override
    public int getItemViewType(int position) {
        return fragmentChat.getMessageContents().get(position).getViewType();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh() {
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (fragmentChat.getMessageContents() == null || fragmentChat.getMessageContents().isEmpty())
            return 0;
        return fragmentChat.getMessageContents().size();
    }

    public void setOnItemLongClickListener(OnItemLongClickListener mLongListener) {
        this.mLongListener = mLongListener;
    }

    public interface OnItemClickListener {
        void onItemClick(MessageContent messageContent, int position);
    }

    public interface OnCheckedChangeListener {
        void onItemCheckedChanged(boolean check, MessageContent messageContent, int position);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener mCheckedChangeListener) {
        this.mCheckedChangeListener = mCheckedChangeListener;
    }

    public void setOnItemClickListener(OnItemClickListener mListener) {
        this.mListener = mListener;
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(MessageContent messageContent, View view, int position);
    }
}
