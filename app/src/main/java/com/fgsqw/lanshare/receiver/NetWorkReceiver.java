package com.fgsqw.lanshare.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;

public class NetWorkReceiver extends BroadcastReceiver {

    private final LANService lanService;

    public NetWorkReceiver(LANService lanService) {
        this.lanService = lanService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int wifi = mWifiInfo.getRssi();//获取wifi信号强度
        if (wifi > -100 && wifi < 0) {
            // 更新设备状态
            lanService.initData();
//            T.s("WiFi已连接");
        } else {
            T.s("WIFI已断开,LANShare可能搜索不到设备");
        }
    }
}

