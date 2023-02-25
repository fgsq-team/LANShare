package com.fgsqw.lanshare.utils;

import android.os.Build;

public class VersionUtils {

    public static boolean isAndroid13() {
        return Build.VERSION.SDK_INT >= 33;
    }

    public static boolean isAndroid11() {
        return Build.VERSION.SDK_INT >= 30;
    }

}
