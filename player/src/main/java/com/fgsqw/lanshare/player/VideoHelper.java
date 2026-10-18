package com.fgsqw.lanshare.player;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * 播放器内部静态工具集：时间格式化、屏幕/网络查询、略缩图、亮度与系统栏控制。
 * 合并了旧实现中的 PlayerUtils 与 LightnessControl 两类职责。
 */
public final class VideoHelper {

    private VideoHelper() {
    }

    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm", Locale.getDefault());

    /** 毫秒 -> "分:秒"，超过一小时显示 "时:分:秒"。 */
    public static String formatTime(long time) {
        long hour = time / 3600000;
        long minute = (time - hour * 3600000) / 60000;
        long second = (time - hour * 3600000 - minute * 60000) / 1000;
        String sHour = hour < 10 ? "0" + hour : "" + hour;
        String sMinute = minute < 10 ? "0" + minute : "" + minute;
        String sSecond = second < 10 ? "0" + second : "" + second;
        return hour > 0 ? sHour + ":" + sMinute + ":" + sSecond : sMinute + ":" + sSecond;
    }

    public static String nowClock() {
        return CLOCK.format(new Date());
    }

    public static int screenWidth(Activity activity) {
        return realPoint(activity).x;
    }

    public static int screenHeight(Activity activity) {
        return realPoint(activity).y;
    }

    private static Point realPoint(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                wm.getDefaultDisplay().getRealSize(point);
            } else {
                wm.getDefaultDisplay().getSize(point);
            }
        }
        return point;
    }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private static ConnectivityManager cm(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = cm(context);
        if (manager == null) {
            return false;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }

    public static boolean isMobile(Context context) {
        ConnectivityManager manager = cm(context);
        if (manager == null) {
            return false;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    /** 提取视频首帧略缩图。 */
    public static Bitmap createThumbnail(String url, int width, int height) {
        Bitmap bitmap;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                retriever.setDataSource(url, new HashMap<String, String>());
            } else {
                retriever.setDataSource(url);
            }
            bitmap = retriever.getFrameAtTime();
            if (bitmap != null && width > 0 && height > 0) {
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            }
        } catch (Exception ex) {
            bitmap = null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
        return bitmap;
    }

    /** 调节窗口亮度，value 为 -255~255 的增量。 */
    public static void adjustBrightness(Activity activity, int value) {
        try {
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            if (lp.screenBrightness < 0) {
                lp.screenBrightness = 0.5f;
            }
            float target = lp.screenBrightness + value / 255.0f;
            lp.screenBrightness = Math.max(0f, Math.min(1f, target));
            activity.getWindow().setAttributes(lp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 当前窗口亮度 0~255。 */
    public static int brightness(Activity activity) {
        float b = activity.getWindow().getAttributes().screenBrightness;
        if (b < 0) {
            return 128;
        }
        return (int) (b * 255f);
    }

    /** 隐藏虚拟导航栏。 */
    public static void hideSystemBars(Activity activity) {
        if (activity == null) {
            return;
        }
        View decor = activity.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            decor.setSystemUiVisibility(View.GONE);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
