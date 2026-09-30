package com.fgsqw.lanshare.activity;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.utils.IOUtil;
import com.fgsqw.lanshare.widget.JpegStreamView;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)

public class TestActivity extends BaseActivity {
    JpegStreamView jpegStreamView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
//        String s = IOUtil.readAssetsTxt(this, "a.html");
        jpegStreamView = findViewById(R.id.ip_cam_view);
        jpegStreamView.start();
//        ThreadUtils.runThread(() -> {
//            MKVtoMP4Converter.convertMKVtoMP4("/sdcard/a.mkv", "/sdcard/b.mp4");
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        jpegStreamView.stop();
    }
}
