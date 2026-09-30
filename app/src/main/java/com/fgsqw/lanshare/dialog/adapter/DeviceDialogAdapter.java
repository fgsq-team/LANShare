package com.fgsqw.lanshare.dialog.adapter;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.dialog.FileInfoDialog;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.utils.CopFileTask;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.util.ArrayList;
import java.util.List;


public class DeviceDialogAdapter extends RecyclerView.Adapter<DeviceDialogAdapter.ViewHolder> {

    private List<Device> deviceList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private Context context;

    public DeviceDialogAdapter(Context context) {
        this.context = context;
    }

    public DeviceDialogAdapter(Context context, List<Device> deviceList) {
        this.deviceList = deviceList;
        this.context = context;
    }

    public boolean onLongClick(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("复制文本");
        String[] items;
        items = new String[]{
                "复制IP",
                "复制设备名",
                "复制网页地址",
                "复制所有",
        };
        ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 绑定选项和点击事件
        builder.setItems(items, (arg0, arg1) -> {
            switch (arg1) {
                case 0:
                    cb.setPrimaryClip(ClipData.newPlainText("text", device.getDevIP()));
                    break;
                case 1:
                    cb.setPrimaryClip(ClipData.newPlainText("text", device.getDevName()));
                    break;
                case 2:
                    cb.setPrimaryClip(ClipData.newPlainText("text", "http://" + device.getDevIP() + ":" + device.getDevPort()));
                    break;
                case 3: {
                    String text = "设备名称: " + device.getDevName() + "\n";
                    text += "设备电量: " + device.getBatteryLevel() + "\n";
                    text += "是否在充电: " + (device.getChargeStatus() == 1 ? "是" : "否") + "\n";
                    text += "设备IP: " + device.getDevIP() + "\n";
                    text += "网页地址: " + "http://" + device.getDevIP() + ":" + device.getDevPort();
                    cb.setPrimaryClip(ClipData.newPlainText("text", text));
                }
                break;
                default:
                    break;
            }
        });
        builder.show();
        return true;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mSelectImg;
        TextView mName;
        TextView mIp;
        TextView tvBattery;
        ImageView mBattery;
        View view;

        public ViewHolder(View view) {
            super(view);
            mName = view.findViewById(R.id.dev_select_name);
            mSelectImg = view.findViewById(R.id.dev_select_img);
            mIp = view.findViewById(R.id.dev_select_ip);
            mBattery = view.findViewById(R.id.dev_select_battery);
            tvBattery = view.findViewById(R.id.dev_tv_battery);
            this.view = view;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.mName.setText(device.getDevName() + " (V" + device.getDataVersion() + ")");
        holder.mIp.setText(device.getDevIP());

        if (device.getDevMode() == Device.UNKNOW) {
            int dp = mUtil.dip2px(context, 40);
            ViewGroup.LayoutParams params = holder.mSelectImg.getLayoutParams();
            params.width = dp;
            params.height = dp;
            holder.mSelectImg.setLayoutParams(params);
        }
        // 设备
        Glide
                .with(context)
                .load(getDeviceDrawable(device))
                .centerCrop()
                .placeholder(R.drawable.ic_null)
                .into(holder.mSelectImg);
        int batteryLevel = device.getBatteryLevel();
        byte chargeStatus = device.getChargeStatus();
//        LLog.debug("batteryLevel: " + batteryLevel);
        int batteryDrawable;
        int batteryColor;
        if (chargeStatus == 1) {
            batteryDrawable = R.drawable.ic_charging;
            batteryColor = R.color.battery4;
        } else {
            if (batteryLevel > 80 && batteryLevel <= 100) {
                batteryDrawable = R.drawable.ic_battery4;
                batteryColor = R.color.battery4;
            } else if (batteryLevel > 60) {
                batteryDrawable = R.drawable.ic_battery3;
                batteryColor = R.color.battery3;
            } else if (batteryLevel > 40) {
                batteryDrawable = R.drawable.ic_battery2;
                batteryColor = R.color.battery2;
            } else if (batteryLevel > 10) {
                batteryDrawable = R.drawable.ic_battery1;
                batteryColor = R.color.battery1;
            } else if (batteryLevel > 0) {
                batteryDrawable = R.drawable.ic_battery0;
                batteryColor = R.color.battery0;
            } else {
                batteryDrawable = R.drawable.ic_null;
                batteryColor = R.color.color_null;
            }
        }
        if (batteryLevel > 0) {
            holder.tvBattery.setVisibility(View.VISIBLE);
            holder.mBattery.setVisibility(View.VISIBLE);
            holder.tvBattery.setText(device.getBatteryLevel() + "%");
            holder.tvBattery.setTextColor(ContextCompat.getColor(context, batteryColor));
            // 电量
            Glide
                    .with(context)
                    .load(batteryDrawable)
                    .centerCrop()
                    .placeholder(R.drawable.ic_null)
                    .into(holder.mBattery);
        } else {
            holder.tvBattery.setVisibility(View.GONE);
            holder.mBattery.setVisibility(View.GONE);
        }
        if (onItemClickListener != null) {
            holder.view.setOnClickListener(v -> onItemClickListener.onClick(device, position));
        }
        if (device.getDevMode() != Device.UNKNOW) {
            holder.view.setOnLongClickListener(v -> DeviceDialogAdapter.this.onLongClick(device));
        }
    }

    private static int getDeviceDrawable(Device device) {
        int devMode = device.getDevMode();
        int dId;
        if (devMode == Device.ANDROID) {
            dId = R.drawable.ic_phone;
        } else if (devMode == Device.WINDOWS) {
            dId = R.drawable.ic_pc;
        } /*else if (devMode == Device.WEB) {
            dId = R.drawable.ic_internet;
        }*/ else {
            dId = R.drawable.ic_launcher;
        }
        return dId;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<Device> deviceList) {
        if (deviceList.hashCode() != this.deviceList.hashCode()) {
            this.deviceList = deviceList;
            notifyDataSetChanged();
        }
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
