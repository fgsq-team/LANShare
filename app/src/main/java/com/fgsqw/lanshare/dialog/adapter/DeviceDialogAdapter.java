package com.fgsqw.lanshare.dialog.adapter;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.Device;

import java.util.List;


public class DeviceDialogAdapter extends RecyclerView.Adapter<DeviceDialogAdapter.ViewHolder> {
    private List<Device> deviceList;

    OnItemClickListener onItemClickListener;

    public static final int NOTIFY_PROGRESS = 1000;
    public static final int NOTIFY_MESSAGE = 1001;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mSelectImg;
        TextView mName;
        TextView mIp;
        View view;


        public ViewHolder(View view) {
            super(view);
            mName = view.findViewById(R.id.dev_select_name);
            mSelectImg = view.findViewById(R.id.dev_select_img);
            mIp = view.findViewById(R.id.dev_select_ip);
            this.view = view;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.mName.setText(device.getDevName());
        holder.mIp.setText(device.getDevIP());
        if (onItemClickListener != null) {
            holder.view.setOnClickListener(v -> onItemClickListener.onClick(device, position));
        }
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public DeviceDialogAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }


    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<Device> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup p1, int viewType) {
        View view = LayoutInflater.from(p1.getContext()).inflate(R.layout.device_select_item, p1, false);
        return new ViewHolder(view);
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public interface OnItemClickListener {
        void onClick(Device device, int position);
    }

}
