package com.fgsqw.lanshare.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.preference.PreferenceManager;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.utils.PrefUtil;

public abstract class BaseActivity extends AppCompatActivity {
    protected PrefUtil prefUtil;
    int topMargin = 0;

    // 翠绿主题模式值（与 SettingActivity 保持一致）
    private static final int THEME_MODE_EMERALD = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefUtil = new PrefUtil(this);
    }

    @Override
    protected void onApplyThemeResource(android.content.res.Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        // 在 AppCompatDelegate 处理完主题后，覆盖为翠绿主题
        int themeMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(PreConfig.THEME_MODE, -1);
        if (themeMode == THEME_MODE_EMERALD) {
            setTheme(R.style.AppTheme_Emerald);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        int activityTopMargin = prefUtil.getInt(PreConfig.ACTIVITY_TOP_MARGIN, 0);
        if (topMargin != activityTopMargin) {
            topMargin = activityTopMargin;
            setTopMargin(topMargin);
        }
    }

    public <T extends View> T bind(int id) {
        return super.findViewById(id);
    }

    public void log(String str) {
        Log.d(this.getClass().getSimpleName(), str);
    }


    public void setTopMargin(int topMargin) {
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setPadding(0, topMargin, 0, 0);
    }
}
