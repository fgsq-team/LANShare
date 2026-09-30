package com.fgsqw.lanshare.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.*;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.toast.T;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * //打开监听引用消息Notification access
 * Intent intent_s = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
 * startActivity(intent_s);
 * 通知同步服务
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
@SuppressLint("OverrideAbstract")
public class LNotifiService extends NotificationListenerService {
    private String nMessage;
    private String TAG = "LNotifiService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.i(TAG, "onNotificationRemoved: " + sbn.getPackageName() + "\n" + sbn.getNotification().tickerText + "\n" + sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Notification notification = sbn.getNotification();
        try {
            //有些通知不能解析出TEXT内容，这里做个信息能判断
            if (Config.SYNC_NOTIFICATION && notification.tickerText != null && !sbn.getPackageName().equals(getPackageName())) {
                Bundle extras = notification.extras;
               /* String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
                int notificationIcon = extras.getInt(Notification.EXTRA_SMALL_ICON);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Icon notificationLargeIcon = (Icon) extras.get(Notification.EXTRA_LARGE_ICON);
                }
                CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
                CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

                String packageName = sbn.getPackageName();*/
                nMessage = notification.tickerText.toString();
                Log.e(TAG, "Get Message" + "-----" + nMessage);
//                T.s(nMessage);
                LANService.getInstance().broadcastMessage(null, nMessage, false, "", false);

            }
        } catch (Exception e) {
            T.s((R.string.unresolvable_notification));
        }

        /*
        *  Bundle extras = sbn.getNotification().extras;

        Log.i(TAG, "onNotificationPosted: " + sbn.getPackageName() + "\n"
                + sbn.getNotification().tickerText + "\n"
                + sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT));*/

    }

}
