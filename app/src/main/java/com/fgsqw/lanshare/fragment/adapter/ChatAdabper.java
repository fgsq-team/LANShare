package com.fgsqw.lanshare.fragment.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.fragment.adapter.viewolder.LeftFileMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.LeftMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.MsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.RightFileMsgHolder;
import com.fgsqw.lanshare.fragment.adapter.viewolder.RightMsgHolder;
import com.fgsqw.lanshare.fragment.FragChat;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.utils.FileUtil;

public class ChatAdabper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;
    private OnItemLongClickListener mLongListener;
    private OnItemClickListener mListener;
    private RequestOptions options;
    private FragChat fragChat;
    private int stateTvColor;

    public ChatAdabper(FragChat fragChat) {
        mContext = fragChat.getContext();
        stateTvColor = mContext.getResources().getColor(R.color.item_text);
        this.fragChat = fragChat;
        mInflater = LayoutInflater.from(mContext);
        options = new RequestOptions().centerCrop();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == ITEM_TYPE.TYPE_LEFT_MSG.ordinal()) {
            return new LeftMsgHolder(mInflater, viewGroup);
        } else if (viewType == ITEM_TYPE.TYPE_LEFT_FILE_MSG.ordinal()) {
            return new LeftFileMsgHolder(mInflater, viewGroup);
        } else if (viewType == ITEM_TYPE.TYPE_RIGHT_FILE_MSG.ordinal()) {
            return new RightFileMsgHolder(mInflater, viewGroup);
        } else {
            return new RightMsgHolder(mInflater, viewGroup);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        MessageContent messageContent = fragChat.getMessageContents().get(i);
        int itemViewType = getItemViewType(i);

        MsgHolder msgHolder = (MsgHolder) viewHolder;
        msgHolder.content.setText(messageContent.getContent());
        Glide.with(mContext).load(messageContent.getHeader())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .into(msgHolder.header);
        if (itemViewType == ITEM_TYPE.TYPE_LEFT_MSG.ordinal()) {
            msgHolder.user.setText(messageContent.getUserName());
        } else if (itemViewType == ITEM_TYPE.TYPE_LEFT_FILE_MSG.ordinal()) {
            LeftFileMsgHolder leftFileMsgHolder = (LeftFileMsgHolder) viewHolder;
            MessageFileContent messageFileContent = (MessageFileContent) messageContent;
            leftFileMsgHolder.content.setText(messageFileContent.getContent());
            leftFileMsgHolder.progressBar.setProgress(messageFileContent.getProgress());
            leftFileMsgHolder.tvSize.setText(FileUtil.computeSize(messageFileContent.getLength()));

            if (messageFileContent.getSuccess() != null) {
                leftFileMsgHolder.progressBar.setVisibility(View.GONE);
                leftFileMsgHolder.stateTv.setVisibility(View.VISIBLE);
                leftFileMsgHolder.stateTv.setText(messageFileContent.getStateMessage());
                if (!messageFileContent.getSuccess()) {
                    leftFileMsgHolder.stateTv.setTextColor(Color.RED);
                } else {
                    leftFileMsgHolder.stateTv.setTextColor(stateTvColor);
                }
            } else {
                leftFileMsgHolder.progressBar.setVisibility(View.VISIBLE);
                leftFileMsgHolder.stateTv.setVisibility(View.GONE);
                leftFileMsgHolder.stateTv.setTextColor(stateTvColor);
            }

        } else if (itemViewType == ITEM_TYPE.TYPE_RIGHT_FILE_MSG.ordinal()) {

            RightFileMsgHolder rightFileMsgHolder = (RightFileMsgHolder) viewHolder;
            MessageFileContent messageFileContent = (MessageFileContent) messageContent;
            rightFileMsgHolder.progressBar.setProgress(messageFileContent.getProgress());
            rightFileMsgHolder.user.setText(messageContent.getToUser() + " ← " + messageContent.getUserName());
            rightFileMsgHolder.tvSize.setText(FileUtil.computeSize(messageFileContent.getLength()));
            if (messageFileContent.getSuccess() != null) {
                rightFileMsgHolder.progressBar.setVisibility(View.GONE);
                rightFileMsgHolder.stateTv.setVisibility(View.VISIBLE);
                rightFileMsgHolder.stateTv.setText(messageFileContent.getStateMessage());
                if (!messageFileContent.getSuccess()) {
                    rightFileMsgHolder.stateTv.setTextColor(Color.RED);
                } else {
                    rightFileMsgHolder.stateTv.setTextColor(stateTvColor);
                }
            } else {
                rightFileMsgHolder.progressBar.setVisibility(View.VISIBLE);
                rightFileMsgHolder.stateTv.setVisibility(View.GONE);
                rightFileMsgHolder.stateTv.setTextColor(stateTvColor);
            }
        } else {
            msgHolder.user.setText(messageContent.getToUser() + " ← " + messageContent.getUserName());
        }

        viewHolder.itemView.setOnClickListener((v) -> {
            if (mListener != null)
                mListener.onItemClick(messageContent, i);
        });
        viewHolder.itemView.setOnLongClickListener((v) -> {
            if (mLongListener != null)
                return mLongListener.onItemLongClick(messageContent, viewHolder.itemView, i);
            return false;
        });
    }

    public enum ITEM_TYPE {
        TYPE_LEFT_MSG,
        TYPE_RIGHT_MSG,
        TYPE_LEFT_FILE_MSG,
        TYPE_RIGHT_FILE_MSG,
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
        if (fragChat.getMessageContents() == null || fragChat.getMessageContents().size() == 0) return 0;
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
