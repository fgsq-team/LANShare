package com.fgsqw.lanshare;

import android.app.Application;

import com.fgsqw.lanshare.utils.PrefUtil;


public class App extends Application {

    private PrefUtil prefUtil;

    public static App app;

    public static App getInstance() {
        return app;
    }

    public static PrefUtil getPrefUtil() {
        return getInstance().prefUtil;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefUtil = new PrefUtil(this);
        app = this;
    }


}
