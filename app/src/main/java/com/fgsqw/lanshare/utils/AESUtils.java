package com.fgsqw.lanshare.utils;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class AESUtils {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    public static String encrypt(String data, String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(padKey(key), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return bytesToHex(encrypted);
    }

    public static String decrypt(String encryptedData, String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(padKey(key), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] original = cipher.doFinal(hexToBytes(encryptedData));
        return new String(original);
    }

    public static String encryptBase64(String data, String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(padKey(key), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    public static String decryptBase64(String encryptedData, String key) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(padKey(key), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] original = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP));
        return new String(original);
    }

    private static byte[] padKey(String key) {
        int keyLength = key.length();
        if (keyLength < 32) {
            int missingLength = 32 - keyLength;
            StringBuilder stringBuilder = new StringBuilder(key);
            for (int i = 0; i < missingLength; i++) {
                stringBuilder.append('\0');
            }
            key = stringBuilder.toString();
        }
        return key.getBytes();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static byte[] hexToBytes(String hexString) {
        int length = hexString.length() / 2;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            int index = i * 2;
            String substring = hexString.substring(index, index + 2);
            result[i] = (byte) Integer.parseInt(substring, 16);
        }
        return result;
    }
}
