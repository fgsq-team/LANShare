package com.fgsqw.lanshare.utils;


import java.text.SimpleDateFormat;
import java.util.Date;

public class LLog {
    public static void info(String str) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = dateFormat.format(new Date());
        format = format + " " + str + "\n";
        FileUtil.writeString(format, "/sdcard/log.txt");
    }
}
