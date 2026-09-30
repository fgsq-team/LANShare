package com.fgsqw.lanshare.pojo.message;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fgsqw.lanshare.fragment.adapter.ChatAdapter;

public class MessageGPSContent extends MessageContent implements Cloneable {

    // 高德
    public static final int TYPE_AMAP = 1;
    // 百度
    public static final int TYPE_BAIDU = 2;
    // 腾讯
    public static final int TYPE_TENCENT = 3;
    // 谷歌
    public static final int TYPE_GOOGLE = 4;

    private int type;
    private String latitude;
    private String longitude;
    private String address;
    private String name;

    public MessageGPSContent(String content) {
        setContent(content);
        JSONObject jsonObject = JSON.parseObject(content);
        latitude = jsonObject.getString("latitude");
        longitude = jsonObject.getString("longitude");
        address = jsonObject.getString("address");
        name = jsonObject.getString("name");
        type = jsonObject.getInteger("type");
    }

    public MessageGPSContent(int type, String latitude, String longitude, String address, String name) {
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.name = name;
        setContent(toString());
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getViewType() {
        return isLeft() ? ChatAdapter.TYPE_GPS_MSG_LEFT : ChatAdapter.TYPE_GPS_MSG_RIGHT;
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("latitude", latitude);
        jsonObject.put("longitude", longitude);
        jsonObject.put("address", address);
        jsonObject.put("name", name);
        return jsonObject.toJSONString();
    }

    @NonNull
    @Override
    public MessageGPSContent clone() {
        return (MessageGPSContent) super.clone();
    }
}
