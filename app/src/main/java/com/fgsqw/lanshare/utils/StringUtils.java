package com.fgsqw.lanshare.utils;

import java.util.UUID;

public class StringUtils {

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
