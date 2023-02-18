package com.fgsqw.lanshare.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.pojo.AddDevice;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.QRcodeUtils;
import com.fgsqw.lanshare.utils.ViewUpdate;
import com.google.gson.Gson;

import java.util.List;

public class DeviceQrCodeActivity extends AppCompatActivity {

    public static boolean exitFlag = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_qrcode);
        exitFlag = false;
        List<Device> mDevice = LANService.getInstance().mDevice;
        AddDevice addDevice = new AddDevice();
        addDevice.setDevices(mDevice);
        addDevice.setKey(Config.KEY);
        String json = new Gson().toJson(addDevice);
        // 二维码
        Bitmap qrcode = QRcodeUtils.qrcode(json, 400, 400);
        ImageView imageView = findViewById(R.id.qr_code);
        imageView.setImageBitmap(qrcode);
        ViewUpdate.runThread(() -> {
            while (!exitFlag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitFlag = false;
    }
}
