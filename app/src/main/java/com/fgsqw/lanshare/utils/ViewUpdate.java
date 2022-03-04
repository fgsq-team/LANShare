package com.fgsqw.lanshare.utils;

import android.os.Handler;
import android.os.Looper;

public class ViewUpdate {

    public static void run(Runnable task, boolean isView) {
        if (isView) {
            try {
                //此处有bug需修复
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(task);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            new Thread(task).start();
        }
    }

    public static void threadUi(Runnable task) {
        run(task, true);
    }

    /**
     * 创建并直接启动线程
     * @param task
     */
    public static void runThread(Runnable task) {
        run(task, false);
    }

}
