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

    private static final String TAG = "ThreadUtils";

    // 全局默认20可用个线程
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(20);

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
            EXECUTOR_SERVICE.submit(task);
            int activeCount = ((ThreadPoolExecutor) EXECUTOR_SERVICE).getActiveCount();
//            Log.i(TAG, "activeThreadCount:" + activeCount);
        }

    }

    public static void threadUi(Runnable task) {
        run(task, true);
    }

    /**
     * 创建并直接启动线程
     *
     */
    public static void runThread(Runnable task) {
        run(task, false);
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        EXECUTOR_SERVICE.shutdown();
    }
}
