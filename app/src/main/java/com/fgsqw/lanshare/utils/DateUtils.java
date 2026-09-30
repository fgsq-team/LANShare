package com.fgsqw.lanshare.utils;

import android.annotation.SuppressLint;

import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    public static String getImageTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        Calendar imageTime = Calendar.getInstance();
        imageTime.setTimeInMillis(time);
        if (sameDay(calendar, imageTime)) {
            return App.getResString(R.string.today);
        } else if (sameYesterday(calendar, imageTime)) {
            return App.getResString(R.string.yesterday);
        } else if (sameBYesterday(calendar, imageTime)) {
            return App.getResString(R.string.day_before_yesterday);
        } else if (sameWeek(calendar, imageTime)) {
            return App.getResString(R.string.this_week);
        } else if (sameMonth(calendar, imageTime)) {
            return App.getResString(R.string.this_month);
        } else {
            return formatDate(new Date(time), "yyyy/MM/dd");//直接显示当前文件时间
        }
    }

    public static String getTimeOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 1 && hour < 6) {
            return "凌晨";
        } else if (hour >= 6 && hour < 12) {
            return "早上";
        } else if (hour >= 12 && hour < 14) {
            return "中午";
        } else if (hour >= 14 && hour < 18) {
            return "下午";
        } else {
            return "晚上";
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

    public static Date parseDate(String dateString, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.parse(dateString);
    }

    // 判断给定时间戳是否表示今天
    public static boolean isToday(long timestamp) {
        Calendar currentDate = Calendar.getInstance(); // 获取当前日期
        currentDate.setTimeInMillis(System.currentTimeMillis());
        Calendar targetDate = Calendar.getInstance(); // 获取目标日期
        targetDate.setTimeInMillis(timestamp);
        return currentDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                currentDate.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR); // 比较日期是否相等
    }

    // 判断给定时间戳是否表示昨天
    public static boolean isYesterday(long timestamp) {
        Calendar currentDate = Calendar.getInstance(); // 获取当前日期
        currentDate.setTimeInMillis(System.currentTimeMillis());
        Calendar targetDate = Calendar.getInstance(); // 获取目标日期
        targetDate.setTimeInMillis(timestamp);
        targetDate.add(Calendar.DAY_OF_YEAR, 1);
        return currentDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                currentDate.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR); // 比较日期是否相差一天
    }

    // 判断给定时间戳是否表示这个月
    public static boolean isCurrentMonth(long timestamp) {
        Calendar currentDate = Calendar.getInstance(); // 获取当前日期
        currentDate.setTimeInMillis(System.currentTimeMillis());
        Calendar targetDate = Calendar.getInstance(); // 获取目标日期
        targetDate.setTimeInMillis(timestamp);
        return currentDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                currentDate.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH); // 比较年份和月份是否相等
    }

    public static boolean isCurrentYear(long timestamp) {
        Calendar currentDate = Calendar.getInstance(); // 获取当前日期
        currentDate.setTimeInMillis(System.currentTimeMillis());
        Calendar targetDate = Calendar.getInstance(); // 获取目标日期
        targetDate.setTimeInMillis(timestamp);

        return currentDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR); // 比较年份是否相等
    }

    // 判断给定时间戳是否表示5分钟之前
    public static boolean isFiveMinutesAgo(long timestamp) {
        Calendar currentTime = Calendar.getInstance(); // 获取当前时间
        Calendar targetTime = Calendar.getInstance(); // 获取目标时间
        targetTime.setTimeInMillis(timestamp);
        // 将目标时间增加5分钟
        targetTime.add(Calendar.MINUTE, 5);
        // 检查目标时间是否在当前时间之前
        return targetTime.before(currentTime);
    }

    public static boolean isFutureDate(long daysToAdd, long timestamp) {
        // 获取当前时间
        Calendar currentCalendar = Calendar.getInstance();
        // 设置时间戳
        currentCalendar.setTimeInMillis(System.currentTimeMillis());
        // 增加指定天数
        currentCalendar.add(Calendar.DAY_OF_YEAR, (int) daysToAdd);
        // 获取增加天数后的时间
        Date futureDate = currentCalendar.getTime();
        // 将时间戳转换为日期
        Date targetDate = new Date(timestamp);
        // 比较是否大于当前时间
        return futureDate.after(targetDate);
    }
}
