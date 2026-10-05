package com.fgsqw.lanshare.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.base.BaseActivity;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.network.NetInfo;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.QrCodeUtils;
import com.fgsqw.lanshare.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DeviceQrCodeActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener {

    public static boolean exitFlag = false;
    private TextView tvIpAddress;
    private ImageView qrCode;
    private CheckBox qrIpv6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_device_qrcode);
        qrCode = bind(R.id.qr_code);
        tvIpAddress = bind(R.id.qr_tv_ip_address);
        qrIpv6 = bind(R.id.qr_ipv6);
//        qrIpv6.setVisibility(View.VISIBLE);
        qrIpv6.setOnCheckedChangeListener(this);
        exitFlag = false;
        updateQrCode(false);
        ThreadUtils.runThread(() -> {
            while (!exitFlag) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignored) {
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


    public void updateQrCode(boolean useIPv6) {
        if (useIPv6) {
            List<NetInfo> ipv6NetInfoList = LANService.getInstance().getDeviceManager().ipv6NetInfoList;
            if (ipv6NetInfoList == null || ipv6NetInfoList.isEmpty()) {
                T.s((R.string.ipv6_address_not_found));
                return;
            }
            NetInfo netInfo = ipv6NetInfoList.get(0);
            String s = Config.KEY + "-1-" + netInfo.getIp() + "-" + Config.FILE_SERVER_PORT;
            Bitmap qrcode = QrCodeUtils.qrcode(s, 400, 400);
            qrCode.setImageBitmap(qrcode);
            tvIpAddress.setText(netInfo.getIp());

        } else {
            Set<Device> localDevices = LANService.getInstance().getDeviceManager().localDevices;
            if (localDevices == null || localDevices.isEmpty()) {
                T.s((R.string.failed_to_get_ip));
                return;
            }
            List<String> devices = new ArrayList<>();
            for (Device localDevice : localDevices) {
                devices.add(localDevice.getDevIP());
            }
            String s = Config.KEY + "-0-" + String.join(",", devices) + "-" + Config.FILE_SERVER_PORT;
            // 二维码
            Bitmap qrcode = QrCodeUtils.qrcode(s, 400, 400);
            qrCode.setImageBitmap(qrcode);
            tvIpAddress.setText(devices.get(0));
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.qr_ipv6) {
            updateQrCode(isChecked);
        }
    }
}
