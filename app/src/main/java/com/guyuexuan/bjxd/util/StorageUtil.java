package com.guyuexuan.bjxd.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guyuexuan.bjxd.model.User;

import java.lang.reflect.Type;
import java.util.List;

public class StorageUtil {
    /**
     * 推荐的免费 AI：智谱 GLM，只让用户填 API Key，其余参数内置
     */
    public static final String DEFAULT_AI_REQUEST_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    public static final String DEFAULT_AI_MODEL = "glm-4.5-flash";
    public static final String DEFAULT_AI_REQUEST_PARAMS = "{\"do_sample\": false}";

    private static final String PREF_NAME = "app_config";
    private static final String KEY_USERS = "users";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_REQUEST_URL = "ai_request_url";
    private static final String KEY_AI_MODEL = "ai_model";
    private static final String KEY_AI_REQUEST_PARAMS = "ai_request_params";
    private static final String KEY_MANUAL_ANSWER = "manual_answer";
    private static final String KEY_SEARCH_ENABLED = "search_enabled";
    private static final String KEY_SEARCH_API_KEY = "search_api_key";
    private static final String KEY_SEARCH_RESULT_COUNT = "search_result_count";
    private static final String KEY_SEARCH_TIMEOUT = "search_timeout";
    private static final String KEY_QINGLONG_REQUEST_URL = "qinglong_request_url";
    private static final String KEY_QINGLONG_CLIENT_ID = "qinglong_client_id";
    private static final String KEY_QINGLONG_CLIENT_SECRET = "qinglong_client_secret";
    private static volatile StorageUtil instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    /**
     * 私有化构造函数，并使用 ApplicationContext 避免泄漏
     *
     * @param context 应用上下文
     */
    private StorageUtil(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        migrateAiSettings();
    }

    /**
     * 单例模式获取实例
     *
     * @param context 应用上下文
     * @return 返回 StorageUtil 实例
     */
    public static StorageUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (StorageUtil.class) {
                if (instance == null) {
                    instance = new StorageUtil(context);
                }
            }
        }
        return instance;
    }

    /**
     * 保存用户列表
     *
     * @param users 用户列表
     */
    public synchronized void saveUserList(List<User> users) {
        String json = gson.toJson(users);
        prefs.edit().putString(KEY_USERS, json).commit();
    }

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    public List<User> getUserList() {
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
        String value = prefs.getString(KEY_AI_REQUEST_URL, "");
        return value.isEmpty() ? DEFAULT_AI_REQUEST_URL : value;
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
        String value = prefs.getString(KEY_AI_MODEL, "");
        return value.isEmpty() ? DEFAULT_AI_MODEL : value;
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
        String value = prefs.getString(KEY_AI_REQUEST_PARAMS, "");
        return value.isEmpty() ? DEFAULT_AI_REQUEST_PARAMS : value;
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
        // URL / 模型 / 参数都有内置默认值（智谱 GLM），用户只需填 API Key
        return !getAiApiKey().isEmpty();
    }

    /**
     * 恢复 AI 参数为推荐值（智谱 GLM，免费），保留用户已填的 API Key
     */
    public void restoreAiDefaults() {
        saveAiRequestUrl(DEFAULT_AI_REQUEST_URL);
        saveAiModel(DEFAULT_AI_MODEL);
        saveAiRequestParams(DEFAULT_AI_REQUEST_PARAMS);
    }

    // ------------------------------------------------------------------
    // 百度 AI 搜索配置
    // ------------------------------------------------------------------

    public boolean isSearchEnabled() {
        return prefs.getBoolean(KEY_SEARCH_ENABLED, true);
    }

    public void setSearchEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SEARCH_ENABLED, enabled).apply();
    }

    public String getSearchApiKey() {
        return prefs.getString(KEY_SEARCH_API_KEY, "");
    }

    public void saveSearchApiKey(String key) {
        prefs.edit().putString(KEY_SEARCH_API_KEY, key).apply();
    }

    public int getSearchResultCount() {
        return prefs.getInt(KEY_SEARCH_RESULT_COUNT, 5);
    }

    public void saveSearchResultCount(int count) {
        prefs.edit().putInt(KEY_SEARCH_RESULT_COUNT, Math.max(1, Math.min(count, 10))).apply();
    }

    public int getSearchTimeout() {
        return prefs.getInt(KEY_SEARCH_TIMEOUT, 30);
    }

    public void saveSearchTimeout(int timeout) {
        prefs.edit().putInt(KEY_SEARCH_TIMEOUT, Math.max(1, timeout)).apply();
    }

    /**
     * 检查青龙面板配置是否完整。
     *
     * @return 如果青龙面板地址、CLIENT_ID 和 CLIENT_SECRET 都已设置（非空），则返回 true；否则返回 false。
     */
    public boolean checkQinglongSettings() {
        String qinglongRequestUrl = getQinglongRequestUrl();
        String qinglongClientId = getQinglongClientId();
        String qinglongClientSecret = getQinglongClientSecret();
        return !qinglongRequestUrl.isEmpty() && !qinglongClientId.isEmpty() && !qinglongClientSecret.isEmpty();
    }

    /**
     * 获取是否启用手动回答
     *
     * @return true 表示启用手动回答，false 表示禁用
     */
    public boolean isManualAnswer() {
        // 默认全自动：配合 AI + 百度搜索答题；需要弹窗/通知手选时再到设置里打开
        return prefs.getBoolean(KEY_MANUAL_ANSWER, false);
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
     * 获取青龙面板地址
     *
     * @return 青龙面板地址
     */
    public String getQinglongRequestUrl() {
        return prefs.getString(KEY_QINGLONG_REQUEST_URL, "");
    }

    /**
     * 保存青龙面板地址
     *
     * @param qinglongRequestUrl 青龙面板地址
     */
    public void saveQinglongRequestUrl(String qinglongRequestUrl) {
        prefs.edit().putString(KEY_QINGLONG_REQUEST_URL, qinglongRequestUrl).apply();
    }

    /**
     * 获取青龙应用 CLIENT_ID
     *
     * @return 青龙应用 CLIENT_ID
     */
    public String getQinglongClientId() {
        return prefs.getString(KEY_QINGLONG_CLIENT_ID, "");
    }

    /**
     * 保存青龙应用 CLIENT_ID
     *
     * @param qinglongClientId 青龙应用 CLIENT_ID
     */
    public void saveQinglongClientId(String qinglongClientId) {
        prefs.edit().putString(KEY_QINGLONG_CLIENT_ID, qinglongClientId).apply();
    }

    /**
     * 获取青龙应用 CLIENT_SECRET
     *
     * @return 青龙应用 CLIENT_SECRET
     */
    public String getQinglongClientSecret() {
        return prefs.getString(KEY_QINGLONG_CLIENT_SECRET, "");
    }

    /**
     * 保存青龙应用 CLIENT_SECRET
     *
     * @param qinglongClientSecret 青龙应用 CLIENT_SECRET
     */
    public void saveQinglongClientSecret(String qinglongClientSecret) {
        prefs.edit().putString(KEY_QINGLONG_CLIENT_SECRET, qinglongClientSecret).apply();
    }

    /**
     * 添加或更新用户
     *
     * @param user 用户对象
     * @return 新用户返回 -1，老用户返回其在列表中的位置
     */
    public synchronized int saveUser(User user) {
        List<User> userList = getUserList();

        // 检查是否已存在
        int position = userList.indexOf(user);
        if (position == -1) {
            userList.add(user);
        } else {
            userList.set(position, user);
        }
        saveUserList(userList);
        return position;
    }
}
