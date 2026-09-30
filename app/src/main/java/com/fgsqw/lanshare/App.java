package com.fgsqw.lanshare;

import android.app.Application;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.File;


public class App extends Application {
    private PrefUtil prefUtil;

    public static App app;

    public static App getInstance() {
        return app;
    }

    public static PrefUtil getPrefUtil() {
        return getInstance().prefUtil;
    }

    public static String getResString(int resid) {
        if (app == null) {
            return "";
        }
        return app.getString(resid);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mUtil.isDebug(this)) {
            Config.SAVE_LOG_PATH = "/sdcard/logs";
            Config.SAVE_LOG = true;
        } else {
            Config.SAVE_LOG_PATH = getCacheDir().getPath() + "/logs";
            Config.SAVE_LOG = false;
        }
        File file = new File(Config.SAVE_LOG_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        LLog.debug("App Create");
        prefUtil = new PrefUtil(this);
        int themeMode = prefUtil.getInt(PreConfig.THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        app = this;
    }


}
