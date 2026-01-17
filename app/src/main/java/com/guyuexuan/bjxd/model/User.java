package com.guyuexuan.bjxd.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class User {
    private final String token;
    private final String nickname;
    private final String phone;
    private final String hid;
    private transient String shareUserHid = "";
    private int order;
    private String addedTime;

    public User(String token, String nickname, String phone, String hid) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
        this.hid = hid;
        this.order = 0;
        this.addedTime = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    public static User fromJson(JsonObject json) {
        String token = json.get("token").getAsString();
        String nickname = json.get("nickname").getAsString();
        String phone = json.get("phone").getAsString();
        String hid = json.get("hid").getAsString();
        return new User(token, nickname, phone, hid);
    }

    public String getToken() {
        return token;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    /**
     * 获取隐藏中间6位数字的手机号
     * 例如：138******34
     */
    public String getMaskedPhone() {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "******" + phone.substring(9);
    }

    public String getHid() {
        return hid;
    }

    public String getShareUserHid() {
        return shareUserHid;
    }

    public void setShareUserHid(String shareUserHid) {
        this.shareUserHid = shareUserHid;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getAddedTime() {
        return addedTime;
    }

    @Deprecated
    public void setAddedTime(String currentTime) {
        this.addedTime = currentTime;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
