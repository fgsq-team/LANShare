package com.fgsqw.lanshare.activity.video;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.FileInfo;
import com.maning.mnvideoplayerlibrary.player.MNViderPlayer;

import java.io.File;
import java.util.Objects;

public class VideoPlayer extends AppCompatActivity {

    private static final String TAG = "MNViderPlayer";

    private MNViderPlayer mnViderPlayer;
    private boolean isInit = false;
    String path;

    protected void fullScann() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT < 19) { // lower api
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
        mnViderPlayer.setIsNeedBatteryListen(true);                                            //设置电量监听
        mnViderPlayer.setOnCompletionListener(mediaPlayer -> Log.i(TAG, "播放完成----"));  // 播放完成监听
    }


    public void start() {
        if (hasPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            //判断本地有没有这个文件
            File file = new File(path);
            if (file.exists()) {
                mnViderPlayer.setDataSource(path, file.getName());
                mnViderPlayer.startVideo();
            } else {
                Toast.makeText(VideoPlayer.this, "文件不存在", Toast.LENGTH_SHORT).show();
            }
        } else {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            Toast.makeText(this, "没有存储权限", Toast.LENGTH_SHORT).show();
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


    public boolean hasPermission(Context context, String permission) {
        int perm = context.checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "存储权限申请成功", Toast.LENGTH_SHORT).show();
                    initPlayer();
                } else {
                    Toast.makeText(this, "存储权限申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
