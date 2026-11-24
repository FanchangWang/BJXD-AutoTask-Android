package com.guyuexuan.bjxd.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import okio.ByteString;

public class User implements Serializable {
    private final String token;
    private final String nickname;
    private final String phone;
    private final String hid;
    private transient String shareUserHid = "";
    private String addedTime;
    private long tokenExpireAt;

    public User(String token, String nickname, String phone, String hid) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
        this.hid = hid;
        this.addedTime = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date());
        this.tokenExpireAt = parseJwtLong(token, "exp") * 1000L;
    }

    /**
     * JWT payload 是 Base64URL，用 Okio 解码（JVM 单测与 Android 均可运行）
     */
    private static byte[] decodeJwtPayload(String encoded) {
        ByteString decoded = ByteString.decodeBase64(encoded.replace('-', '+').replace('_', '/'));
        if (decoded == null) throw new IllegalArgumentException("Invalid JWT payload");
        return decoded.toByteArray();
    }

    /**
     * JWT payload 中取 exp（秒），解析失败返回 0
     */
    public static long parseJwtLong(String token, String key) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return 0L;
            byte[] payload = decodeJwtPayload(parts[1]);
            JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * JWT payload 中的 user_hid
     */
    public static String parseJwtHid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";
            byte[] payload = decodeJwtPayload(parts[1]);
            JsonObject json = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.has("user_hid") ? json.get("user_hid").getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static User fromJson(JsonObject json) {
        if (json == null || json.isJsonNull()) {
            throw new IllegalArgumentException("User data is missing");
        }
        String token = json.has("token") && !json.get("token").isJsonNull() ? json.get("token").getAsString() : "";
        String nickname = json.has("nickname") && !json.get("nickname").isJsonNull() ? json.get("nickname").getAsString() : "";
        String phone = json.has("phone") && !json.get("phone").isJsonNull() ? json.get("phone").getAsString() : "";
        String hid = json.has("hid") && !json.get("hid").isJsonNull() ? json.get("hid").getAsString() : "";
        if (token.isEmpty() || phone.isEmpty()) {
            throw new IllegalArgumentException("User data is incomplete (token/phone missing)");
        }
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

    /**
     * addedTime 实际语义是「最近更新时间」，同步时会用它跟青龙 updatedAt 比较
     */
    public void setAddedTime(String currentTime) {
        this.addedTime = currentTime;
    }

    /**
     * token 过期时间（epoch millis）；旧数据没有该字段时从 token 实时解析
     */
    public long getTokenExpireAt() {
        if (tokenExpireAt <= 0L) {
            tokenExpireAt = parseJwtLong(token, "exp") * 1000L;
        }
        return tokenExpireAt;
    }

    public boolean isTokenExpired() {
        long exp = getTokenExpireAt();
        return exp > 0L && System.currentTimeMillis() > exp;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
