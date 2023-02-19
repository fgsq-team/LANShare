package com.fgsqw.lanshare.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.util.concurrent.TimeUnit;

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
        ViewUpdate.runThread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                lanService.initData();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}

