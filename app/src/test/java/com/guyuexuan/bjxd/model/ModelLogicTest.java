package com.guyuexuan.bjxd.model;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 纯逻辑单测：JWT 解析与用户数据校验，无需网络
 */
public class ModelLogicTest {

    private static String jwtWithPayload(String payload) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return header + "." + body + ".signature";
    }

    @Test
    public void parseJwt_readsExpAndHid() {
        String token = jwtWithPayload("{\"user_hid\":\"abc123\",\"exp\":1798498400}");
        assertEquals(1798498400L, User.parseJwtLong(token, "exp"));
        assertEquals("abc123", User.parseJwtHid(token));
    }

    @Test
    public void parseJwt_invalidTokenReturnsEmpty() {
        assertEquals(0L, User.parseJwtLong("not-a-jwt", "exp"));
        assertEquals("", User.parseJwtHid("not-a-jwt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromJson_rejectsIncompleteData() {
        JsonObject json = JsonParser.parseString("{\"nickname\":\"张三\"}").getAsJsonObject();
        User.fromJson(json);
    }

    @Test
    public void fromJson_acceptsCompleteData() {
        JsonObject json = JsonParser.parseString(
                "{\"token\":\"t\",\"nickname\":\"张三\",\"phone\":\"13800000000\",\"hid\":\"h\"}"
        ).getAsJsonObject();
        User user = User.fromJson(json);
        assertEquals("13800000000", user.getPhone());
        assertEquals("138******00", user.getMaskedPhone());
    }
}
