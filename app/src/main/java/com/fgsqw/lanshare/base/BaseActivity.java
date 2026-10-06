package com.fgsqw.lanshare.base;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.preference.PreferenceManager;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.InfoDialog;
import com.fgsqw.lanshare.utils.PrefUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseActivity extends AppCompatActivity {
    protected PrefUtil prefUtil;
    int topMargin = 0;

    /** 当前前台Activity（静态），供全局弹窗使用 */
    private static volatile Activity sCurrentActivity;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

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

    @Override
    protected void onResume() {
        super.onResume();
        sCurrentActivity = this;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sCurrentActivity == this) {
            sCurrentActivity = null;
        }
    }

    /**
     * 获取当前前台Activity（供全局判断使用）
     */
    public static Activity getCurrentActivity() {
        return sCurrentActivity;
    }

    /**
     * 显示绘图同步确认对话框（可在任意Activity界面弹出）
     * <p>使用当前前台Activity作为上下文，调用线程阻塞等待用户选择。</p>
     *
     * @param deviceName 请求方设备名称
     * @param timeoutSec 超时秒数
     * @return true=接受, false=拒绝或超时
     */
    public static boolean showDrawSyncConfirm(String deviceName, int timeoutSec) {
        Activity activity = sCurrentActivity;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        sHandler.post(() -> {
            Activity act = sCurrentActivity;
            if (act == null || act.isFinishing() || act.isDestroyed()) {
                latch.countDown();
                return;
            }
            InfoDialog dialog = new InfoDialog(act);
            dialog.setTitle("绘图同步请求");
            dialog.setText("设备 \"" + deviceName + "\" 请求与您同步绘图，是否接受？");
            dialog.setCancelable(false);
            dialog.setLeftButtonText("拒绝");
            dialog.setRightButtonText("接受");
            dialog.setOnClickListener(agree -> {
                result.set(agree);
                latch.countDown();
            });
            dialog.setOnDismissListener(d -> latch.countDown());
            dialog.show();
        });
        try {
            latch.await(timeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return result.get();
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
