package com.fgsqw.lanshare.utils;

import android.annotation.TargetApi;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;

public class NotificationUtils {
    private static final int NOTIFICATION_ID = 0x1989;
    private static final String CHANNEL_ID = "lanshare_channel_id";
    private static final String CHANNEL_NAME = "LANShare消息通知服务";
    private static final String CHANNEL_DESCRIPTION = "LANShare Channel Description";


    /**
     * 使服务更好的运行在后台， 不被销毁（手机内存低时不优先销毁）
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void showBackendNotification(Context context, String title, String content) {
        if (!(context instanceof Service)) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, DataCenterActivity.class);
        // notification
        Notification notification = null;
        // 构建 PendingIntent
        PendingIntent pi = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pi = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pi = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        //版本兼容
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setAutoCancel(false)
                    .setContentIntent(pi)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new Notification.Builder(context)
                    .setAutoCancel(false)
                    .setContentIntent(pi)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setContentIntent(pi)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setWhen(System.currentTimeMillis())
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, title, importance);
            mChannel.setDescription(content);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            mChannel.setSound(Uri.EMPTY, audioAttributes);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setContentIntent(pi)
                    .build();
        }
        ((Service) context).startForeground(NOTIFICATION_ID, notification);
    }


    public static void showNotification(Context context, String title, String content) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // 创建通知渠道
        createNotificationChannel(notificationManager);
        // 设置点击通知时的跳转 Intent
        Intent intent = new Intent(context, DataCenterActivity.class); // 替换成你的主 Activity 类
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }

        // 创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent) // 设置点击通知时的跳转操作
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        // 显示通知
        notificationManager.notify(1, builder.build());
    }

    private static void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            // 通知静音
            channel.setSound(Uri.EMPTY, audioAttributes);
            // 提示灯
            channel.enableLights(true);
            // 显示Logo
            channel.setShowBadge(true);
            // 设置锁屏可见
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // 设置通知渠道的声音为静音
            channel.setSound(Uri.EMPTY, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());
//            channel.setSound(null, null);
            channel.setDescription(CHANNEL_DESCRIPTION);
            // 注册通道
            notificationManager.createNotificationChannel(channel);
        }
    }


}
