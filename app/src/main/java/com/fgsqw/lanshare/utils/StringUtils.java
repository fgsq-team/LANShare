package com.fgsqw.lanshare.utils;

import java.util.UUID;

public class StringUtils {

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 填充数字为指定长度
     *
     * @param numberString  原始数字
     * @param desiredLength 填充的指定长度
     */
    public static String padWithZeroes(String numberString, int desiredLength) {
        if (numberString.length() >= desiredLength) {
            return numberString;
        }
        int zeroesToAdd = desiredLength - numberString.length();
        StringBuilder paddedNumber = new StringBuilder();
        for (int i = 0; i < zeroesToAdd; i++) {
            paddedNumber.append("0");
        }
        paddedNumber.append(numberString);
        return paddedNumber.toString();
    }
}
