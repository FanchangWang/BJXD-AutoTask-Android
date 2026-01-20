package com.guyuexuan.bjxd.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class User implements Serializable{
    private final String token;
    private final String nickname;
    private final String phone;
    private final String hid;
    private transient String shareUserHid = "";
    private String addedTime;

    public User(String token, String nickname, String phone, String hid) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
        this.hid = hid;
        this.addedTime = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    public static User fromJson(JsonObject json) {
        String token = json.get("token").getAsString();
        String nickname = json.get("nickname").getAsString();
        String phone = json.get("phone").getAsString();
        String hid = json.get("hid").getAsString();
        return new User(token, nickname, phone, hid);
    }

    // 重写equals：仅按phone判定相等
    @Override
    public boolean equals(Object o) {
        // 1. 引用相同，直接返回true（自反性）
        if (this == o) return true;
        // 2. 空值/类型不同，返回false（非空性+对称性）
        if (o == null || getClass() != o.getClass()) return false;
        // 3. 强转后比较核心字段（用Objects.equals避免NPE）
        User user = (User) o;
        return Objects.equals(phone, user.phone);
    }

    // 【必写】重写equals必须同时重写hashCode（否则HashMap/HashSet异常）
    @Override
    public int hashCode() {
        // 仅用核心字段phone计算hash值
        return Objects.hash(phone);
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
