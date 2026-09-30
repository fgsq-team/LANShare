package com.fgsqw.lanshare.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.App;
import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.config.Config;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.message.MessageContent;
import com.fgsqw.lanshare.constants.LCmd;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.LLog;
import com.fgsqw.lanshare.utils.StringUtils;

import java.util.Map;

/**
 * 自定义广播接收
 */
public class LANShareReceiver extends BroadcastReceiver {
    private static final String TAG = "LANShareReceiver";
    public static final String LANSHARE_EXPORT_INERFACE = "com.fgsqw.lanshare.LANSHARE_EXPORT_INERFACE";
    public static final String LANSHARE_BORADCAST_EXPORT_INERFACE = "com.fgsqw.lanshare.LANSHARE_BORADCAST_EXPORT_INERFACE";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean receiveBroadcastMessages = App.getPrefUtil().getBoolean(PreConfig.RECEIVE_BROADCAST_MESSAGES);
        if (receiveBroadcastMessages) {
            try {
                String content = intent.getStringExtra("content");
                JSONObject jsonObject = JSON.parseObject(content);
                String ip = jsonObject.getString("ip");
                String msg = jsonObject.getString("msg");
                boolean isClip = jsonObject.getBooleanValue("isClip");
                String packageName = jsonObject.getString("packageName");
                if (StringUtils.isEmpty(msg) || StringUtils.isEmpty(packageName)) {
                    return;
                }
                LANService instance = LANService.getInstance();
                if (instance != null) {
                    // 保存消息到消息列表
                    MessageContent messageContent = new MessageContent();
                    messageContent.setId(StringUtils.getUUID());
                    messageContent.setLeft(false);
                    messageContent.setContent(msg);
                    messageContent.setUserName(LANService.getInstance().getDevName());
                    messageContent.setToUser(StringUtils.isEmpty(ip) ? context.getString(R.string.all_devices) : ip);
                    Message mMessage = Message.obtain();
                    mMessage.what = LCmd.SERVICE_ADD_MESSGAGE;
                    mMessage.obj = messageContent;
                    instance.messageSend(mMessage);

                    if (StringUtils.isEmpty(ip)) {
                        instance.broadcastMessage(null, msg, isClip, packageName);
                    } else {
                        // 如果ip在在线列表中匹配就使用在线列表设备
                        Map<String, Device> onLineDevices = instance.getOnLineDevices();
                        for (Device value : onLineDevices.values()) {
                            if (value.getDevIP().equals(ip)) {
                                instance.broadcastMessage(value, msg, isClip, packageName);
                                return;
                            }
                        }
                        // ip不在列表中新建一个设备
                        Device device = new Device();
                        device.setDevIP(ip);
                        device.setDevPort(Config.DEFAULT_FILE_SERVER_PORT);
                        instance.broadcastMessage(device, msg, isClip, packageName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                T.s((R.string.broadcast_service_reception_exception));
                LLog.error("error", e);
            }
        }
    }


}
