package com.fgsqw.lanshare.dialog.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.StorageInfo;

import java.util.ArrayList;
import java.util.List;


public class SelectStorageDialogAdapter extends RecyclerView.Adapter<SelectStorageDialogAdapter.ViewHolder> {

    private List<StorageInfo> storageInfos = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    public static final int NOTIFY_PROGRESS = 1000;
    public static final int NOTIFY_MESSAGE = 1001;

    public SelectStorageDialogAdapter() {
    }

    public SelectStorageDialogAdapter(List<StorageInfo> storageInfos) {
        this.storageInfos = storageInfos;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView storageName;
        TextView size;

        public ViewHolder(View view) {
            super(view);
            storageName = view.findViewById(R.id.storage_name);
            size = view.findViewById(R.id.storage_size);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorageInfo storageInfo = storageInfos.get(position);
        holder.storageName.setText(storageInfo.getName());
        holder.size.setText(storageInfo.getPath());
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(storageInfo, position));
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<StorageInfo> storageInfos) {
        if (storageInfos.hashCode() != this.storageInfos.hashCode()) {
            this.storageInfos = storageInfos;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int viewType) {
        View view = LayoutInflater.from(p1.getContext()).inflate(R.layout.storage_select_item, p1, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return storageInfos.size();
    }

    public interface OnItemClickListener {
        void onClick(StorageInfo storageInfo, int position);
    }

}
