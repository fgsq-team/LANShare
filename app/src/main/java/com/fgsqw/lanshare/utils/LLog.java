package com.fgsqw.lanshare.utils;


import android.util.Log;

import com.fgsqw.lanshare.config.Config;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LLog {
    private static final String TAG = "LLog";

    public static void info(String str) {
        Log.i(TAG, str);
        if(Config.SAVE_LOG){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String format = dateFormat.format(new Date());
            format = format + " " + str;
            format += "\n";
            FileUtil.writeString(format, "/sdcard/log.txt");
        }
    }
}
