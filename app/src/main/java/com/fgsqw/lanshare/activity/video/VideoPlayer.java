package com.fgsqw.lanshare.activity.video;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.pojo.file.FileInfo;
import com.fgsqw.lanshare.toast.T;
import com.maning.mnvideoplayerlibrary.player.MNViderPlayer;

import java.io.File;
import java.util.Objects;

public class VideoPlayer extends BaseActivity {

    private static final String TAG = "MNViderPlayer";

    private MNViderPlayer mnViderPlayer;
    private boolean isInit = false;
    String path;

    protected void fullScann() {
        // 隐藏标题
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT < 19) {
            View v = getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            //for new api versions.这种方式虽然是官方推荐，但是根本达不到效果
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏
        fullScann();
        setContentView(R.layout.video_player);
        path = Objects.requireNonNull(getIntent().getExtras())
                .getString("path");
        initViews();
        initPlayer();
        start();

    }

    public static void toPreviewVideoActivity(Activity activity, FileInfo images) {
        if (images != null) {
            File name = new File(images.getPath());
            Intent intent = new Intent(activity, VideoPlayer.class);
            intent.putExtra("path", name.getPath());
            activity.startActivity(intent);
//            dataCenterActivity.overridePendingTransition(0, 0);
        }
    }

    private void initViews() {
        mnViderPlayer = findViewById(R.id.mn_videoplayer);
    }

    private void initPlayer() {
        if (isInit) {
            return;
        }
        isInit = true;
        // 设置电量监听
        mnViderPlayer.setIsNeedBatteryListen(true);
        // 播放完成监听
        mnViderPlayer.setOnCompletionListener(mediaPlayer -> Log.i(TAG, "播放完成----"));
    }


    public void start() {
        //判断本地有没有这个文件
        File file = new File(path);
        if (file.exists()) {
            mnViderPlayer.setDataSource(path, file.getName());
            mnViderPlayer.startVideo();
        } else {
            T.s((R.string.file_not_found));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //暂停
        mnViderPlayer.pauseVideo();
    }

    @Override
    public void onBackPressed() {
        if (mnViderPlayer.isFullScreen()) {
            mnViderPlayer.setOrientationPortrait();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        //一定要记得销毁View
        if (mnViderPlayer != null) {
            mnViderPlayer.destroyVideo();
            mnViderPlayer = null;
        }
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                T.s((R.string.storage_permission_request_successful));
                initPlayer();
            } else {
                T.s((R.string.storage_permission_request_failed));
            }
        }
    }

}
