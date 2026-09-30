package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

public class BatteryUtil {

    /**
     * 获取设备当前电池电量百分比（支持 Android 4.4 及以上版本）
     *
     * @param context 应用上下文
     * @return 当前电池电量百分比（0-100），获取失败返回 -1
     */
    public static int[] getBatteryLevel(Context context) {
        int[] batteryBytes = new int[4];
        batteryBytes[0] = -1;
        batteryBytes[1] = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0+ 使用 BatteryManager API 检查电池容量
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                // 检查充电状态需要通过广播实现，因为 BATTERY_PROPERTY_STATUS 从 API 26 才开始可用
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, intentFilter);
                if (batteryStatus != null) {
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                        batteryBytes[1] = 1;
                    }
                }
                // 返回电池容量
                batteryBytes[0] = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } else {
            // Android 4.4 - Android 5.0 使用 ACTION_BATTERY_CHANGED 广播
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                // 检查设备是否正在充电
                if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                    batteryBytes[1] = 1;
                }
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                // 计算电量百分比
                if (level >= 0 && scale > 0) {
                    batteryBytes[0] = (int) ((level / (float) scale) * 100);
                }
            }
        }
        return batteryBytes;
    }

}
