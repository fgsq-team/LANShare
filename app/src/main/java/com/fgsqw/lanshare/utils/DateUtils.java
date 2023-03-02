package com.fgsqw.lanshare.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateUtils {

    public static String getImageTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Calendar imageTime = Calendar.getInstance();
        imageTime.setTimeInMillis(time);
        if (sameDay(calendar, imageTime)) {
            return "今天";
        } else if (sameYesterday(calendar, imageTime)) {
            return "昨天";
        } else if (sameBYesterday(calendar, imageTime)) {
            return "前天";
        } else if (sameWeek(calendar, imageTime)) {
            return "本周";
        } else if (sameMonth(calendar, imageTime)) {
            return "本月";
        } else {
            return formatDate(new Date(time), "yyyy/MM/dd");//直接显示当前文件时间
        }
    }

    public static boolean sameDay(Calendar calendar1, Calendar calendar2) {//今天

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);

    }

    public static boolean sameYesterday(Calendar calendar1, Calendar calendar2) {//昨天

        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) > calendar2.get(Calendar.DAY_OF_YEAR)) {
            if (calendar1.get(Calendar.DAY_OF_YEAR) - calendar2.get(Calendar.DAY_OF_YEAR) == 1) {
                return true;
            }
        }

        return false;
    }

    public static boolean sameBYesterday(Calendar calendar1, Calendar calendar2) {//前天
        if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) > calendar2.get(Calendar.DAY_OF_YEAR)) {
            if (calendar1.get(Calendar.DAY_OF_YEAR) - calendar2.get(Calendar.DAY_OF_YEAR) == 2) {
                return true;
            }
        }
        return false;
    }

    public static boolean sameWeek(Calendar calendar1, Calendar calendar2) {//本周
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.WEEK_OF_YEAR) == calendar2.get(Calendar.WEEK_OF_YEAR);
    }

    public static boolean sameMonth(Calendar calendar1, Calendar calendar2) {//本月
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
    }


    @SuppressLint("SimpleDateFormat")
    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

}
