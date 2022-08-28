package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.viewolder.FileMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.MediaMsgHloder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.MsgHolder;
import com.fgsqw.lanshare.fragment.FragChat;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.pojo.MessageFolderContent;
import com.fgsqw.lanshare.pojo.MessageMediaContent;
import com.fgsqw.lanshare.utils.FileUtil;

import java.util.Objects;

public class ChatAdabper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_MSG_LEFT = 1;
    public static final int TYPE_MSG_RIGHT = 2;
    public static final int TYPE_FILE_MSG_LEFT = 3;
    public static final int TYPE_FILE_MSG_RIGHT = 4;
    public static final int TYPE_MEDIA_MSG_LEFT = 5;
    public static final int TYPE_MEDIA_MSG_RIGHT = 6;

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final RequestOptions options;
    private final FragChat fragChat;
    private final int stateTvColor;

    private OnItemLongClickListener mLongListener;
    private OnItemClickListener mListener;

    public ChatAdabper(FragChat fragChat) {
        mContext = fragChat.getContext();
        stateTvColor = Objects.requireNonNull(mContext).getResources().getColor(R.color.item_text);
        this.fragChat = fragChat;
        mInflater = LayoutInflater.from(mContext);
        options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        MsgHolder msgHolder;
        if (viewType == TYPE_MSG_LEFT) {
            msgHolder = new MsgHolder(true, mInflater, viewGroup);
        } else if (viewType == TYPE_MSG_RIGHT) {
            msgHolder = new MsgHolder(false, mInflater, viewGroup);
        } else if (viewType == TYPE_FILE_MSG_LEFT) {
            msgHolder = new FileMsgHolder(true, mInflater, viewGroup);
        } else if (viewType == TYPE_FILE_MSG_RIGHT) {
            msgHolder = new FileMsgHolder(false, mInflater, viewGroup);
        } else if (viewType == TYPE_MEDIA_MSG_LEFT) {
            msgHolder = new MediaMsgHloder(true, mInflater, viewGroup);
        } else/* if (viewType == TYPE_MEDIA_MSG_RIGHT)*/ {
            msgHolder = new MediaMsgHloder(false, mInflater, viewGroup);
        }
        return msgHolder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        MessageContent messageContent = fragChat.getMessageContents().get(position);

        MsgHolder msgHolder = (MsgHolder) viewHolder;
        // 头像
        Glide.with(mContext).load(messageContent.getHeader())
                .apply(options)
                .into(msgHolder.header);
        // 设置消息
        msgHolder.content.setText(messageContent.getContent());

        // 设置用户名
        if (messageContent.isLeft()) {
            msgHolder.user.setText(messageContent.getUserName());
        } else {
            msgHolder.user.setText(messageContent.getToUser() + " ← " + messageContent.getUserName());
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
    }


    public int getDataPosition(MessageContent messageContent) {
        return fragChat.getMessageContents().indexOf(messageContent);
    }


    @Override
    public int getItemViewType(int position) {
        return fragChat.getMessageContents().get(position).getViewType();
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
        if (fragChat.getMessageContents() == null || fragChat.getMessageContents().size() == 0)
            return 0;
        return fragChat.getMessageContents().size();
    }

    public void setOnItemLongClickListener(OnItemLongClickListener mLongListener) {
        this.mLongListener = mLongListener;
    }

    public interface OnItemClickListener {
        void onItemClick(MessageContent messageContent, int position);
    }

    public void setOnItemClickListener(OnItemClickListener mListener) {
        this.mListener = mListener;
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(MessageContent messageContent, View view, int position);
    }
}
