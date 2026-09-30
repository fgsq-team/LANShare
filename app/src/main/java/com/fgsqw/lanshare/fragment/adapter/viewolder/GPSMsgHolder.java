package com.fgsqw.lanshare.fragment.adapter.viewolder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.fgsqw.lanshare.pojo.message.MessageGPSContent;

public class GPSMsgHolder extends MsgHolder {

    public GPSMsgHolder(boolean isLeft, LayoutInflater mInflater, ViewGroup viewGroup) {
        super(isLeft, mInflater, viewGroup);
    }

    /**
     * 绑定 GPS 位置数据，在气泡中显示名称+地址，点击跳转地图。
     */
    public void bindGPS(MessageGPSContent gps) {
        String name = gps.getName();
        String address = gps.getAddress();
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isEmpty()) {
            sb.append(name);
        }
        if (address != null && !address.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(address);
        }
        if (sb.length() == 0) {
            sb.append("位置: ").append(gps.getLatitude()).append(", ").append(gps.getLongitude());
        }
        setContentText(sb.toString());
    }

    /**
     * 根据经纬度打开系统地图（如果有地图应用）。
     */
    public static void openMap(Context context, MessageGPSContent gps) {
        try {
            double lat = Double.parseDouble(gps.getLatitude());
            double lng = Double.parseDouble(gps.getLongitude());
            String label = gps.getName() != null ? gps.getName() : "位置";
            // geo: 协议，Android 系统会弹出地图应用选择
            Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 没有地图应用或解析失败，静默忽略
        }
    }
}
