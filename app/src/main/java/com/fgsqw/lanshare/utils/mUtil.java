package com.fgsqw.lanshare.utils;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;

import java.lang.reflect.Field;
import java.util.Random;

public class mUtil {


    public static void copyString(String content, Context context) {
// 得到剪贴板管理器
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(content.trim());
    }

    /**
     * 使服务更好的运行在后台， 不被销毁（手机内存低时不优先销毁）
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void showNotification(Context context, String title, String content) {
        if (!(context instanceof Service)) {
            return;
        }
        final int NOTIFICATION_ID = 0x1989;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //准备intent
        Intent intent = new Intent();
        String action = context.getPackageName() + ".action";
        intent.setAction(action);

        //notification
        Notification notification = null;

        // 构建 PendingIntent
        PendingIntent pi = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //版本兼容

        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= LOLLIPOP_MR1) {
            notification = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setAutoCancel(false)
                    .setContentIntent(pi).build();

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= LOLLIPOP_MR1) {
            notification = new Notification.Builder(context)
                    .setAutoCancel(false)
                    .setContentIntent(pi)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .build();

        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, title, importance);
            mChannel.setDescription(content);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);

            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(context, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(content)
                    .setContentTitle(title)
                    .setContentText(content)
                    .build();
        }
        ((Service) context).startForeground(NOTIFICATION_ID, notification);
    }


    public static String StringSize(String str, int i) {
        String getstr = null;
        if (str.length() > i) {
            getstr = str.substring(0, i - 1);
            getstr += "..";
        } else {
            return str;
        }
        return getstr;
    }

    public static String addString(Object... obj) {
        if (obj != null && obj.length > 0) {
            StringBuffer sb = new StringBuffer();
            for (Object o : obj) {
                sb.append(o);
            }
            return sb.toString();
        }
        return null;
    }

    public static int dip2px1(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int random(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    /**
     * 获取程序版本
     *
     * @param context
     * @return
     */
    public static String getAppVersionName(Context context) {
        String versionName = null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
        }
        return versionName;
    }

    /**
     * 获取自身apk路径
     **/
    public static String getSelfApkPath(Context context) {
        String appDir = null;
        try {
            //通过包名获取程序源文件路径
            appDir = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appDir;
    }

}
