package com.fgsqw.lanshare.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fgsqw.lanshare.toast.T;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadUtils {

    private static final String TAG = "ViewUpdate";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(20);


    public static void run(Runnable task, boolean isView) {
        if (isView) {
            try {
                // 此处更新UI时会闪退待解决
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            executorService.submit(task);
            int activeCount = ((ThreadPoolExecutor) executorService).getActiveCount();
            Log.i(TAG, "activeCount:" + activeCount);
        }

    }

    public static void threadUi(Runnable task) {
        run(task, true);
    }

    /**
     * 创建并直接启动线程
     *
     * @param task
     */
    public static void runThread(Runnable task) {
        run(task, false);
    }

}
