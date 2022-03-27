package com.fgsqw.lanshare.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.widget.JpegStreamView;

public class TestActivity extends AppCompatActivity {
    JpegStreamView jpegStreamView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        jpegStreamView = findViewById(R.id.ip_cam_view);
        jpegStreamView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        jpegStreamView.start();
    }
}
