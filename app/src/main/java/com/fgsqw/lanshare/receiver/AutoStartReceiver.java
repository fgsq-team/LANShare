package com.fgsqw.lanshare.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.PrefUtil;


public class AutoStartReceiver extends BroadcastReceiver {
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 是否接收广播消息
        boolean autoStart = App.getPrefUtil().getBoolean(PreConfig.AUTO_START);
//        LLog.info("开机启动:" + intent.getAction() + " autoStart:" + autoStart);
        if (autoStart && BOOT_COMPLETED.equals(intent.getAction())) {
            Config.initConfig(new PrefUtil(context));
            Intent mainIntent = new Intent(context, LANService.class);
            mainIntent.putExtra(PreConfig.AUTO_START, true);
            context.startService(mainIntent);
        }
    }

}
