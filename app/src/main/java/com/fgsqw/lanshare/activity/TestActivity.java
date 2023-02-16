package com.fgsqw.lanshare.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.pojo.AddDevice;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.NetWorkUtil;
import com.fgsqw.lanshare.utils.QRcodeUtils;
import com.fgsqw.lanshare.widget.JpegStreamView;
import com.google.gson.Gson;

import java.util.List;

public class TestActivity extends AppCompatActivity {
//    JpegStreamView jpegStreamView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_qrcode);
//        jpegStreamView = findViewById(R.id.ip_cam_view);
//        jpegStreamView.start();

        List<Device> mDevice = LANService.service.mDevice;
        AddDevice addDevice = new AddDevice();
        addDevice.setDevices(mDevice);
        String s = new Gson().toJson(addDevice);

        Bitmap qrcode = QRcodeUtils.qrcode(s, 400, 400);
        ImageView imageView = findViewById(R.id.qr_code);
        imageView.setImageBitmap(qrcode);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        jpegStreamView.stop();


    }
}
