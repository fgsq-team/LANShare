package com.fgsqw.lanshare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefUtil {
    private SharedPreferences preferences;

    public PrefUtil(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void saveInt(String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void saveString(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void saveLong(String key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void saveBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public int getInt(String key) {
        return preferences.getInt(key, -1);
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public long getLong(String key) {
        return preferences.getLong(key, -1);
    }

    public long getLong(String key, long devValue) {
        return preferences.getLong(key, devValue);
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }


}
