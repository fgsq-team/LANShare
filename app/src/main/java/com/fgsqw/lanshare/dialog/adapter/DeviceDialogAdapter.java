package com.fgsqw.lanshare.dialog.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.Device;

import java.util.List;


public class DeviceDialogAdapter extends RecyclerView.Adapter<DeviceDialogAdapter.ViewHolder> {
    private List<Device> deviceList;

    OnItemClickListener onItemClickListener;

    public static final int NOTIFY_PROGRESS = 1000;
    public static final int NOTIFY_MESSAGE = 1001;

    Context context;


    public DeviceDialogAdapter(Context context, List<Device> deviceList) {
        this.deviceList = deviceList;
        this.context = context;

    }


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
        int devMode = device.getDevMode();
        int dId;
        if (devMode == Device.ANDROID) {
            dId = R.drawable.ic_phone;
        } else if (devMode == Device.WIN) {
            dId = R.drawable.ic_win;
        } else {
            dId = R.drawable.ic_launcher;
        }

        Glide
                .with(context)
                .load(dId)
                .centerCrop()
                .placeholder(R.drawable.ic_null)
                .into(holder.mSelectImg);

        if (onItemClickListener != null) {
            holder.view.setOnClickListener(v -> onItemClickListener.onClick(device, position));
        }
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
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
