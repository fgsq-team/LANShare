package com.fgsqw.lanshare.utils;


import android.annotation.SuppressLint;
import android.util.Log;

import com.fgsqw.lanshare.config.Config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LLog {
    private static final String TAG = "LLog";

    public static String getTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer = stringWriter.getBuffer();
        return buffer.toString();
    }

    @SuppressLint("SimpleDateFormat")
    public static void error(String str, Throwable throwable) {
        try {
            throwable.printStackTrace();
            Log.i(TAG, str);
            if (Config.SAVE_LOG) {
                Date date = new Date();
                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
                String day = dayFormat.format(date);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String format = dateFormat.format(new Date());
                format = format + " " + str + ": " + getTrace(throwable);
                format += "\n";
                FileUtil.writeString(format, Config.SAVE_LOG_PATH + "/error_" + day + ".txt");
            }
        } catch (Exception ignored) {
        }
    }

    public static void error(Throwable throwable) {
        error("", throwable);
    }

    @SuppressLint("SimpleDateFormat")
    public static void debug(String str) {
        try {
            Log.d(TAG, str);
            if (Config.SAVE_LOG) {
                Date date = new Date();
                SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
                String day = dayFormat.format(date);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String format = dateFormat.format(new Date());
                format = format + " " + str;
                format += "\n";
                FileUtil.writeString(format, Config.SAVE_LOG_PATH + "/info_" + day + ".txt");
            }
        } catch (Exception ignored) {
        }
    }
}
