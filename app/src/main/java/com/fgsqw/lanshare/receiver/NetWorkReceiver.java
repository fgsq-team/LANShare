package com.fgsqw.lanshare.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.fgsqw.lanshare.pojo.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.lang.reflect.Method;

public class NetWorkReceiver extends BroadcastReceiver {

    private static final String TAG = "NetWorkReceiver";

    private final LANService lanService;

    public NetWorkReceiver(LANService lanService) {
        this.lanService = lanService;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent.getAction());
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ViewUpdate.runThread(() -> {
                try {
                    Thread.sleep(1000);
                    lanService.initData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            lanService.initData();
        } else if (NetWorkUtil.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int state = intent.getIntExtra(NetWorkUtil.EXTRA_WIFI_AP_STATE, 0);
            if (state == NetWorkUtil.WIFI_AP_STATE_ENABLED) { // 13 热点已开启 有时候开启后还是不能获取到热点ip 这里只能暂时用着笨办法循环5次获取，获取不到就真没办法了
                ViewUpdate.runThread(() -> {
                    try {
                        Thread.sleep(1000);
                        lanService.initData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else if (state == NetWorkUtil.WIFI_AP_STATE_DISABLED) { // 11 热点已关闭
                lanService.initData();
            }/* else if (state == WIFI_AP_STATE_DISABLING) { // 10  热点正在关闭...
            } else if (state == WIFI_AP_STATE_ENABLING) { // 12 热点正在打开...
            } else if (state == WIFI_AP_STATE_FAILED) { // 14  热点操作失败... 硬件不支持或者其他问题
            }*/
        }
    }
}

