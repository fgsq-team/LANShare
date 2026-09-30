package com.fgsqw.lanshare.utils;

import android.os.Build;

public class VersionUtils {

    public static boolean isAfterAndroid13() {
        return Build.VERSION.SDK_INT >= 33;
    }

    public static boolean isAfterAndroid11() {
        return Build.VERSION.SDK_INT >= 30;
    }

    public static boolean isAfterAndroid12() {
        return Build.VERSION.SDK_INT >= 31;
    }

}
