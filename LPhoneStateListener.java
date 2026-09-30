package com.fgsqw.lanshare.listener;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.utils.ThreadUtils;

public class LPhoneStateListener extends PhoneStateListener {

    boolean fastInit = true;

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        if (fastInit) {
            fastInit = false;
            return;
        }
        if (App.getPrefUtil().getBoolean(PreConfig.SEND_MUTE)) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d("LPhoneStateListener", "挂断");
                    ThreadUtils.runThread(() -> LANService.getInstance().noticeDeviceRestoreMedia());
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d("LPhoneStateListener", "接听");
                    ThreadUtils.runThread(() -> LANService.getInstance().noticeDeviceMuteMedia());
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d("LPhoneStateListener", "响铃");
                    ThreadUtils.runThread(() -> LANService.getInstance().noticeDeviceMuteMedia());
                    //输出来电号码
                    break;
            }
        }
    }

}
