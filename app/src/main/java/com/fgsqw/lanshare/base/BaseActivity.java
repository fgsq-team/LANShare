package com.fgsqw.lanshare.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.utils.PrefUtil;

public abstract class BaseActivity extends AppCompatActivity {
    protected PrefUtil prefUtil;
    int topMargin = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefUtil = new PrefUtil(this);
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
