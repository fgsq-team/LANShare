package com.fgsqw.lanshare.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class NetWorkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int wifi = mWifiInfo.getRssi();//获取wifi信号强度
        if (wifi > -100 && wifi < 0) {
            Toast.makeText(context, "WiFi已连接", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "WIFI已断开", Toast.LENGTH_LONG).show();
        }
    }
}

