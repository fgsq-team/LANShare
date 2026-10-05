package com.fgsqw.lanshare.listener;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.DeviceDataScanner;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.util.concurrent.TimeUnit;

/**
 * 媒体库变动监听
 */
public class ImageObserver extends ContentObserver {
    private static final String TAG = "ImageObserver";
    private final LANService lanService;
    boolean registered = false;

    public ImageObserver(LANService lanService, Handler handler) {
        super(handler);
        this.lanService = lanService;
    }

    public void onChange(boolean selfChange, Uri uri, int flags) {
        super.onChange(selfChange, uri, flags);
        if (uri == null || flags != ContentResolver.NOTIFY_INSERT) {
            return;
        }
        Log.d(TAG, flags + " " + uri.getPath());
        ThreadUtils.runThread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignored) {
            }
            DeviceDataScanner.scanImages(lanService, true);
            lanService.syncMedia();
        });
    }


    // 注册观察者
    public void registerObserver() {
        if (registered) {
            return;
        }
        ContentResolver contentResolver = lanService.getContentResolver();
        contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
        );
        contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                this
        );
        registered = true;
    }

    // 取消注册观察者
    public void unregisterObserver() {
        if (!registered) {
            return;
        }
        lanService.getContentResolver().unregisterContentObserver(this);
        registered = false;
    }

}
