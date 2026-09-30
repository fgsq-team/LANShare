package com.fgsqw.lanshare.activity;


import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.utils.CameraUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends BaseActivity implements SurfaceHolder.Callback {

    SurfaceView surfaceView;
    CameraUtils cameraUtils;
    SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);
        surfaceView = findViewById(R.id.camera_surfaceView);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        cameraUtils = new CameraUtils(surfaceView.getHolder(), 8880);
        LinearLayout.LayoutParams lp = new
                LinearLayout.LayoutParams(surfaceView.getLayoutParams());
        lp.height = cameraUtils.mPreviewWidth;
        lp.width = cameraUtils.mPreviewHeight;
        surfaceView.setLayoutParams(lp);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraUtils.stop();
    }
}
