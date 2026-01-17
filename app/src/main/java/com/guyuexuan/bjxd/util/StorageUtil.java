package com.guyuexuan.bjxd.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guyuexuan.bjxd.model.User;

import java.lang.reflect.Type;
import java.util.List;

public class StorageUtil {
    private static final String PREF_NAME = "app_config";
    private static final String KEY_USERS = "users";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_REQUEST_URL = "ai_request_url";
    private static final String KEY_AI_MODEL = "ai_model";
    private static final String KEY_AI_REQUEST_PARAMS = "ai_request_params";
    private static final String KEY_MANUAL_ANSWER = "manual_answer";
    private final SharedPreferences prefs;
    private final Gson gson;

    /**
     * 构造函数
     *
     * @param context 上下文对象
     */
    public StorageUtil(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        migrateAiSettings();
    }

    /**
     * 保存用户列表
     *
     * @param users 用户列表
     */
    public void saveUsers(List<User> users) {
        String json = gson.toJson(users);
        prefs.edit().putString(KEY_USERS, json).apply();
    }

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    public List<User> getUsers() {
        String json = prefs.getString(KEY_USERS, "[]");
        Type type = new TypeToken<List<User>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    /**
     * 获取 API 密钥
     *
     * @return API 密钥
     */
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    /**
     * 保存 API 密钥
     *
     * @param apiKey API 密钥
     */
    @Deprecated
    public void saveApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    /**
     * 获取 AI API 密钥
     *
     * @return AI API 密钥
     */
    public String getAiApiKey() {
        return prefs.getString(KEY_AI_API_KEY, "");
    }

    /**
     * 保存 AI API 密钥
     *
     * @param aiApiKey AI API 密钥
     */
    public void saveAiApiKey(String aiApiKey) {
        prefs.edit().putString(KEY_AI_API_KEY, aiApiKey).apply();
    }

    /**
     * 获取 AI 请求 URL
     *
     * @return AI 请求 URL
     */
    public String getAiRequestUrl() {
        return prefs.getString(KEY_AI_REQUEST_URL, "");
    }

    /**
     * 保存 AI 请求 URL
     *
     * @param aiRequestUrl AI 请求 URL
     */
    public void saveAiRequestUrl(String aiRequestUrl) {
        prefs.edit().putString(KEY_AI_REQUEST_URL, aiRequestUrl).apply();
    }

    /**
     * 获取 AI 模型名称
     *
     * @return AI 模型名称
     */
    public String getAiModel() {
        return prefs.getString(KEY_AI_MODEL, "");
    }

    /**
     * 保存 AI 模型名称
     *
     * @param aiModel AI 模型名称
     */
    public void saveAiModel(String aiModel) {
        prefs.edit().putString(KEY_AI_MODEL, aiModel).apply();
    }

    /**
     * 获取 AI 请求参数
     *
     * @return AI 请求参数 JSON 字符串
     */
    public String getAiRequestParams() {
        return prefs.getString(KEY_AI_REQUEST_PARAMS, "");
    }

    /**
     * 保存 AI 请求参数
     *
     * @param aiRequestParams AI 请求参数 JSON 字符串
     */
    public void saveAiRequestParams(String aiRequestParams) {
        prefs.edit().putString(KEY_AI_REQUEST_PARAMS, aiRequestParams).apply();
    }

    /**
     * 迁移旧版 AI 模型配置到新版
     * 当 AI API 密钥为空而旧版 API 密钥存在时，将旧版 API 密钥迁移到 AI API 密钥
     * 并设置默认的 AI 请求 URL、模型和请求参数
     */
    public void migrateAiSettings() {
        String apiKey = getApiKey();
        String aiApiKey = getAiApiKey();
        if (!apiKey.isEmpty() && aiApiKey.isEmpty()) {
            saveAiApiKey(apiKey);
            saveAiRequestUrl("https://api.hunyuan.cloud.tencent.com/v1/chat/completions");
            saveAiModel("hunyuan-turbo");
            saveAiRequestParams("{\"enable_enhancement\": true, \"force_search_enhancement\": true, \"enable_instruction_search\": true}");
        }
    }


    /**
     * 检查 AI 相关配置是否完整。
     *
     * @return 如果 AI API 密钥、请求 URL 和模型名称都已设置（非空），则返回 true；否则返回 false。
     */
    public boolean checkAiSettings() {
        String aiApiKey = getAiApiKey();
        String aiRequestUrl = getAiRequestUrl();
        String aiModel = getAiModel();
        return !aiApiKey.isEmpty() && !aiRequestUrl.isEmpty() && !aiModel.isEmpty();
    }

    /**
     * 获取是否启用手动回答
     *
     * @return true 表示启用手动回答，false 表示禁用
     */
    public boolean isManualAnswer() {
        return prefs.getBoolean(KEY_MANUAL_ANSWER, true);
    }

    /**
     * 设置是否启用手动回答
     *
     * @param enabled true 表示启用手动回答，false 表示禁用
     */
    public void setManualAnswer(boolean enabled) {
        prefs.edit().putBoolean(KEY_MANUAL_ANSWER, enabled).apply();
    }

    /**
     * 添加或更新用户
     * 如果用户已存在（根据手机号判断），则更新用户信息并保持原有顺序
     * 如果用户不存在，则添加新用户，顺序设为列表长度
     *
     * @param user 用户对象
     */
    public void addUser(User user) {
        List<User> users = getUsers();
        // 检查是否已存在
        boolean isUpdated = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getPhone().equals(user.getPhone())) {
                // 更新已存在的用户信息，保持原有的 order
                user.setOrder(users.get(i).getOrder());
                users.set(i, user);
                isUpdated = true;
                break;
            }
        }

        if (!isUpdated) {
            // 添加新用户，order 设为列表长度
            user.setOrder(users.size());
            users.add(user);
        }

        saveUsers(users);
    }
}
