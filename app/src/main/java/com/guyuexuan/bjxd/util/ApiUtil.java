package com.guyuexuan.bjxd.util;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.guyuexuan.bjxd.model.QinglongEnv;
import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * API工具类，封装了与服务器交互的所有HTTP请求方法
 * <p>
 * 主要功能包括：
 * - 用户信息获取
 * - 积分查询
 * - 任务状态查询
 * - 签到相关操作
 * - 文章浏览与积分提交
 * - 答题相关操作
 * - AI问答功能
 * <p>
 * 使用OkHttpClient作为HTTP客户端，Gson用于JSON解析
 * </p>
 */
public class ApiUtil {
    private static final String BASE_URL = "https://bm2-api.bluemembers.com.cn";
    private static final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS) // 2 秒无法建立 TCP 连接就放弃
            .readTimeout(10, TimeUnit.SECONDS) // 10 秒无响应就放弃
            .writeTimeout(10, TimeUnit.SECONDS) // 10 秒无响应就放弃
            .retryOnConnectionFailure(true) // dns 解析多个 ip 时，按 ipv6...ipv4... 顺序尝试，直到某个 ip 成功或全部失败
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 最大 1 个连接，保持 5 分钟
            .build();
    // 新增：为 AI 请求创建一个专用的、长超时的 Client
    private static final OkHttpClient aiClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS) // 连接超时可以适当放宽
            .readTimeout(5, TimeUnit.MINUTES) // 读取超时设置为3分钟，等待AI响应
            .writeTimeout(5, TimeUnit.MINUTES) // 写入超时也设置为3分钟
            .retryOnConnectionFailure(true).build();

    // 百度 AI 搜索（千帆）专用客户端：给出比业务接口更宽松的超时
    private static final OkHttpClient searchClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final Gson gson = new Gson();
    /**
     * 百度千帆 AI 搜索接口，与 Python 版 WebSearcher 保持一致
     */
    private static final String BAIDU_SEARCH_URL = "https://qianfan.baidubce.com/v2/ai_search/web_search";
    /**
     * API 端点
     * <p>
     * 定义了与服务器交互的所有端点路径
     * </p>
     */
    private static final String API_USER_INFO = "/v1/app/account/users/info";
    private static final String API_MY_SCORE = "/v1/app/user/my_score";
    private static final String API_TASK_LIST = "/v1/app/user/task/list";
    private static final String API_SIGN_LIST = "/v1/app/user/reward_list";
    private static final String API_SIGN_SUBMIT = "/v1/app/user/reward_report";
    private static final String API_ARTICLE_LIST = "/v1/app/white/article/list2";
    private static final String API_ARTICLE_DETAIL = "/v1/app/white/article/detail_app/%s";
    private static final String API_TASK_SCORE = "/v1/app/score";
    private static final String API_QUESTION_INFO = "/v1/app/special/daily/ask_info";
    private static final String API_QUESTION_SUBMIT = "/v1/app/special/daily/ask_answer";
    private static final String API_QINGLONG_AUTH = "/open/auth/token";
    private static final String API_QINGLONG_ENVS = "/open/envs";

    /**
     * 停止任务时中断所有在途请求（AI 读超时可达 5 分钟），兜底取消
     */
    public static void cancelAllCalls() {
        client.dispatcher().cancelAll();
        aiClient.dispatcher().cancelAll();
        searchClient.dispatcher().cancelAll();
    }

    /**
     * 获取带有默认请求头的Request.Builder
     * <p>
     * 自动添加token和device请求头
     * </p>
     *
     * @param token 用户认证token
     * @return 带有默认请求头的Request.Builder实例
     */
    @NonNull
    private static Request.Builder getRequestBuilder(String token) {
        return new Request.Builder().addHeader("token", token).addHeader("device", "mp"); // 添加device=mp头
    }

    /**
     * 把响应体解析为 JsonObject；空体/非对象/畸形 JSON 一律抛带端点名的 IOException
     */
    private static JsonObject parseObject(Response response, String endpoint) throws IOException {
        ResponseBody body = response.body();
        if (body == null) throw new IOException(endpoint + ": empty response body");
        String json = body.string();
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            throw new IOException(endpoint + ": empty response");
        }
        try {
            JsonObject object = gson.fromJson(json, JsonObject.class);
            if (object == null || object.isJsonNull()) {
                throw new IOException(endpoint + ": response is not a JSON object");
            }
            return object;
        } catch (JsonSyntaxException e) {
            throw new IOException(endpoint + ": malformed JSON response", e);
        }
    }

    /**
     * 必需字段：缺失或 JSON null 时抛明确异常，避免平台类型 NPE
     */
    private static JsonElement required(JsonObject object, String key, String endpoint) throws IOException {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            throw new IOException(endpoint + ": missing field " + key);
        }
        return element;
    }

    private static String readMsg(JsonObject object, String endpoint) {
        JsonElement msg = object.get("msg");
        if (msg == null || msg.isJsonNull()) {
            JsonElement message = object.get("message");
            msg = message == null || message.isJsonNull() ? null : message;
        }
        return msg == null ? endpoint + ": server returned an error" : msg.getAsString();
    }

    /**
     * 获取用户信息
     *
     * @param token 用户认证token
     * @return 用户信息对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    @NonNull
    public static User getUserInfo(String token) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + API_USER_INFO).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject jsonObject = parseObject(response, "getUserInfo");
                if (required(jsonObject, "code", "getUserInfo").getAsInt() == 0) {
                    JsonObject data = required(jsonObject, "data", "getUserInfo").getAsJsonObject();
                    // 手动添加 token，因为 data 中没有这个字段，而 User 类需要它
                    data.addProperty("token", token);
                    return User.fromJson(data);
                } else {
                    throw new IOException(readMsg(jsonObject, "getUserInfo"));
                }
            } else {
                if (response.code() == 403) {
                    throw new IOException("Code 403: Token 已过期，请重新添加账号");
                } else {
                    throw new IOException("请求失败: " + response.code());
                }
            }
        }
    }

    /**
     * 获取用户积分信息
     *
     * @param token 用户认证token
     * @return 积分信息JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject getScore(String token) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + API_MY_SCORE + "?page_no=1&page_size=5").build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                if (response.code() == 403) {
                    throw new IOException("Code 403: Token 已过期，请重新添加账号");
                } else {
                    throw new IOException("请求失败: " + response.code());
                }
            }
        }
    }

    /**
     * 获取任务状态信息
     * <p>
     * 包括签到、浏览文章、答题等任务的完成状态
     * </p>
     *
     * @param token 用户认证token
     * @return 任务状态对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    @NonNull
    public static TaskStatus getTaskStatus(String token) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + API_TASK_LIST).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    TaskStatus status = new TaskStatus();

                    // 检查签到任
                    if (data.has("action4")) {
                        status.setSignCompleted(data.getAsJsonObject("action4").get("status").getAsInt() == 1);
                    }

                    // 检查浏览文章任务
                    if (data.has("action12")) {
                        status.setViewCompleted(data.getAsJsonObject("action12").get("status").getAsInt() == 1);
                    }

                    // 检查答题任务
                    if (data.has("action39")) {
                        status.setQuestionCompleted(data.getAsJsonObject("action39").get("status").getAsInt() == 1);
                    }

                    return status;
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 获取签到信息
     * <p>
     * 包括签到所需的hid和hash等参数，用于后续的签到提交
     * </p>
     *
     * @param token 用户认证token
     * @return 签到信息JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject getSignInfo(String token) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + API_SIGN_LIST).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 提交签到信息
     *
     * @param token      用户认证token
     * @param hid        签到任务ID
     * @param rewardHash 签到奖励哈希值
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static void submitSign(String token, String hid, String rewardHash) throws IOException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("hid", hid);
        jsonBody.addProperty("hash", rewardHash);
        jsonBody.addProperty("sm_deviceId", "");
        jsonBody.add("ctu_token", null);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token).url(BASE_URL + API_SIGN_SUBMIT).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() != 0) {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 获取文章列表
     * <p>
     * 用于获取可浏览的文章列表，支持分页
     * </p>
     *
     * @param token 用户认证token
     * @return 文章列表JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject getArticleList(String token) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + API_ARTICLE_LIST + "?page_no=1&page_size=20&type_hid=").build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 浏览文章详情
     * <p>
     * 用于标记文章已浏览，完成浏览文章任务
     * </p>
     *
     * @param token     用户认证token
     * @param articleId 文章ID
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static void viewArticle(String token, String articleId) throws IOException {
        Request request = getRequestBuilder(token).url(BASE_URL + String.format(API_ARTICLE_DETAIL, articleId)).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() != 0) {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 提交文章浏览积分
     * <p>
     * 在完成文章浏览后调用，用于获取相应积分
     * </p>
     *
     * @param token 用户认证token
     * @return 积分获取结果JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject submitArticleScore(String token) throws IOException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("ctu_token", "");
        jsonBody.addProperty("action", 12);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token).url(BASE_URL + API_TASK_SCORE).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 获取每日答题信息
     * <p>
     * 包括题目内容、选项、正确答案等信息
     * </p>
     *
     * @param token 用户认证token
     * @return 答题信息JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject getQuestionInfo(String token) throws IOException {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        Request request = getRequestBuilder(token).url(BASE_URL + API_QUESTION_INFO + "?date=" + date).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 提交答题答案
     * <p>
     * 用于提交每日答题的答案，可选择分享给其他用户
     * </p>
     *
     * @param token        用户认证token
     * @param questionId   题目ID
     * @param answer       答案内容
     * @param shareUserHid 分享用户ID（可选）
     * @return 答题结果JSON对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static JsonObject submitQuestionAnswer(String token, String questionId, String answer, String shareUserHid) throws IOException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("answer", answer);
        jsonBody.addProperty("questions_hid", questionId);
        jsonBody.addProperty("ctu_token", "");

        // 如果有 shareUserHid，添加日期和分享用户 hid
        if (shareUserHid != null && !shareUserHid.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            jsonBody.addProperty("date", sdf.format(new Date()));
            jsonBody.addProperty("share_user_hid", shareUserHid);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token).url(BASE_URL + API_QUESTION_SUBMIT).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    return jsonObject.getAsJsonObject("data");
                } else {
                    throw new IOException(jsonObject.get("msg").getAsString());
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 百度 AI 搜索（千帆）：按题干检索资料，返回可直接拼进 prompt 的文本。
     * <p>
     * 与 Python 版 WebSearcher 行为一致：任何异常都只记日志并返回空串，
     * 绝不影响签到等主任务。免费额度每月 1500 次。
     * </p>
     *
     * @param apiKey         百度 bce-v3 API Key
     * @param resultCount    搜索结果条数（1-10）
     * @param timeoutSeconds 读取超时秒数
     * @param query          搜索关键词（题干）
     * @param logger         日志回调，可为 null
     * @return 资料文本；失败或无结果返回空串
     */
    public static String searchBaidu(String apiKey, int resultCount, int timeoutSeconds,
                                     String query, Consumer<String> logger) {
        if (apiKey == null || apiKey.trim().isEmpty() || query == null || query.trim().isEmpty()) {
            return "";
        }

        int topK = Math.max(1, Math.min(resultCount, 10));
        String keyword = query.length() > 70 ? query.substring(0, 70) : query;

        JsonObject payload = new JsonObject();
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", keyword);
        messages.add(message);
        payload.add("messages", messages);
        payload.addProperty("search_source", "baidu_search_v2");

        JsonArray resourceFilter = new JsonArray();
        JsonObject webFilter = new JsonObject();
        webFilter.addProperty("type", "web");
        webFilter.addProperty("top_k", topK);
        resourceFilter.add(webFilter);
        payload.add("resource_type_filter", resourceFilter);

        Request request = new Request.Builder()
                .url(BAIDU_SEARCH_URL)
                .addHeader("Authorization", "Bearer " + apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8")))
                .build();

        OkHttpClient clientWithTimeout = searchClient.newBuilder()
                .readTimeout(Math.max(1, timeoutSeconds), TimeUnit.SECONDS)
                .build();
        try (Response response = clientWithTimeout.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logSearch(logger, "⚠️ 百度搜索 HTTP " + response.code() + "，仅用题目询问 AI");
                return "";
            }

            JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
            int code = 0;
            if (result.has("code") && !result.get("code").isJsonNull()) {
                try {
                    code = result.get("code").getAsInt();
                } catch (Exception ignored) {
                    code = -1;
                }
            }
            if (code != 0) {
                String msg = result.has("message") ? result.get("message").getAsString() : "";
                logSearch(logger, "⚠️ 百度搜索返回错误码 " + code + ": " + msg);
                return "";
            }

            JsonArray references = result.getAsJsonArray("references");
            if (references == null || references.isEmpty()) {
                logSearch(logger, "⚠️ 联网搜索未取得有效资料，仅用题目询问 AI");
                return "";
            }

            StringBuilder material = new StringBuilder();
            int count = Math.min(topK, references.size());
            for (int i = 0; i < count; i++) {
                JsonObject item = references.get(i).getAsJsonObject();
                String content = firstNonBlank(item, "content", "description");
                if (content.isEmpty()) {
                    continue;
                }
                String title = firstNonBlank(item, "title");
                String site = firstNonBlank(item, "website", "site_name");
                String header = site.isEmpty() ? title : (title.isEmpty() ? site : title + "（" + site + "）");
                if (!header.isEmpty()) {
                    material.append(header).append('\n');
                }
                material.append(content).append("\n\n");
                if (material.length() > 8000) {
                    break;
                }
            }

            String text = material.toString().trim();
            if (text.isEmpty()) {
                logSearch(logger, "⚠️ 联网搜索未取得有效资料，仅用题目询问 AI");
                return "";
            }
            if (text.length() > 8000) {
                text = text.substring(0, 8000);
            }
            logSearch(logger, String.format(Locale.getDefault(),
                    "📚 联网搜索取得 %d 字资料（%d 条结果）", text.length(), count));
            return text;
        } catch (Exception e) {
            logSearch(logger, "⚠️ 联网搜索失败（" + e.getMessage() + "），仅用题目询问 AI");
            return "";
        }
    }

    private static void logSearch(Consumer<String> logger, String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }

    private static String firstNonBlank(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                String value = obj.get(key).getAsString().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * 调用AI API进行问答
     * <p>
     * 支持自定义模型、请求参数和API地址，用于获取AI生成的回答
     * </p>
     *
     * @param aiApiKey        AI API密钥
     * @param aiRequestUrl    AI API请求地址
     * @param aiModel         AI模型名称
     * @param aiRequestParams AI请求参数（JSON格式，可选）
     * @param question        问题内容
     * @return AI生成的回答
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static String askAI(String aiApiKey, String aiRequestUrl, String aiModel, String aiRequestParams, String question) throws IOException {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", aiModel); // 使用参数传入的 model
        JsonArray messagesArr = new JsonArray();
        JsonObject messageSystemObj = new JsonObject();
        messageSystemObj.addProperty("role", "system");
        messageSystemObj.addProperty("content", "你是一位北京现代汽车品牌的专家，对车型配置非常熟悉。\n以下是一道单选题，请只从题目实际列出的选项里选择正确答案。\n注意：题目可能只给出 2 个或 3 个选项，并非永远 4 个。\n请仅输出对应选项的那个英文字母，不要输出任何其他字符。");
        messagesArr.add(messageSystemObj);
        JsonObject messageUserObj = new JsonObject();
        messageUserObj.addProperty("role", "user");
        messageUserObj.addProperty("content", question);
        messagesArr.add(messageUserObj);
        jsonBody.add("messages", messagesArr);

        // 如果 aiRequestParams 不为空，尝试解析并添加到 jsonBody
        if (aiRequestParams != null && !aiRequestParams.isEmpty()) {
            try {
                JsonObject params = gson.fromJson(aiRequestParams, JsonObject.class);
                for (String key : params.keySet()) {
                    jsonBody.add(key, params.get(key));
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }

        Request request = new Request.Builder().url(aiRequestUrl).addHeader("Authorization", "Bearer " + aiApiKey).post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"))).build();

        try (Response response = aiClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                JsonElement choices = required(jsonObject, "choices", "askAI");
                if (!choices.isJsonArray() || choices.getAsJsonArray().isEmpty()) {
                    throw new IOException("askAI: empty choices");
                }
                JsonObject message = choices.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("message");
                if (message == null || message.isJsonNull()) {
                    throw new IOException("askAI: missing message");
                }
                JsonElement content = message.get("content");
                if (content == null || content.isJsonNull() || content.getAsString().isEmpty()) {
                    content = message.get("reasoning_content");
                }
                return content == null || content.isJsonNull() ? "" : content.getAsString();
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    /**
     * 获取青龙面板认证令牌
     * <p>
     * 通过客户端ID和客户端密钥调用青龙面板API获取访问令牌，用于后续青龙面板操作的认证
     * </p>
     *
     * @param base_url      青龙面板基础URL
     * @param client_id     青龙面板客户端ID
     * @param client_secret 青龙面板客户端密钥
     * @return 青龙面板访问令牌
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static String getQinglongToken(String base_url, String client_id, String client_secret) throws IOException {
        // 用 HttpUrl 编码参数，避免 secret 中的 & + # 等字符截断或注入
        HttpUrl parsed = HttpUrl.parse(base_url + API_QINGLONG_AUTH);
        if (parsed == null) {
            throw new IOException("Invalid Qinglong base URL: " + base_url);
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("client_id", client_id)
                .addQueryParameter("client_secret", client_secret)
                .build();

        // 创建请求，添加 Accept 头
        Request request = new Request.Builder().url(url).get().addHeader("Accept", "application/json").build();

        // 使用 client 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                // 解析 JSON 响应
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 200) {
                    // 提取 token 并返回
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    return data.get("token").getAsString();
                } else {
                    // 处理业务错误
                    throw new IOException("API Error: " + jsonObject.get("code").getAsString() + " - " + (jsonObject.has("msg") ? jsonObject.get("msg").getAsString() : "Unknown error"));
                }
            } else {
                // 处理 HTTP 错误
                throw new IOException("Request failed: " + response.code());
            }
        }
    }

    /**
     * 获取青龙面板环境变量
     * <p>
     * 调用青龙面板API获取所有包含"BJXD"关键字的环境变量，用于管理北京现代相关的环境配置
     * </p>
     *
     * @param base_url    青龙面板基础URL
     * @param token       青龙面板访问令牌
     * @param searchValue 搜索关键字
     * @return 环境变量数组，包含所有符合条件的环境变量
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static QinglongEnv[] getQinglongEnvList(String base_url, String token, String searchValue) throws IOException {
        // 构建完整 URL，包含 searchValue 参数
        HttpUrl parsed = HttpUrl.parse(base_url + API_QINGLONG_ENVS);
        if (parsed == null) {
            throw new IOException("Invalid Qinglong base URL: " + base_url);
        }
        HttpUrl url = parsed.newBuilder()
                .addQueryParameter("searchValue", searchValue)
                .build();

        // 创建请求，添加 Accept 头和 Authorization 头
        Request request = new Request.Builder().url(url).get().addHeader("Accept", "application/json").addHeader("Authorization", "Bearer " + token).build();

        // 使用 client 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                // 解析 JSON 响应
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 200) {
                    // 获取 data 节点
                    JsonElement dataElement = jsonObject.get("data");

                    // 关键：直接将 dataElement 转换为 QinglongEnv 数组
                    QinglongEnv[] envs = gson.fromJson(dataElement, QinglongEnv[].class);
                    if (envs == null) {
                        throw new IOException("getQinglongEnvList: empty data");
                    }
                    return envs;
                } else {
                    // 处理业务错误
                    throw new IOException("API Error: " + jsonObject.get("code").getAsString() + " - " + (jsonObject.has("msg") ? jsonObject.get("msg").getAsString() : "Unknown error"));
                }
            } else {
                // 处理 HTTP 错误
                throw new IOException("Request failed: " + response.code());
            }
        }
    }

    /**
     * 创建青龙面板环境变量
     * <p>
     * 调用青龙面板API创建新的环境变量，用于添加北京现代相关的配置信息
     * </p>
     *
     * @param base_url 青龙面板基础URL
     * @param token    青龙面板访问令牌
     * @param name     环境变量名称
     * @param value    环境变量值
     * @param remarks  环境变量备注
     * @return 创建的环境变量对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static QinglongEnv createQinglongEnv(String base_url, String token, String name, String value, String remarks) throws IOException {
        // 构建完整 URL
        String url = base_url + API_QINGLONG_ENVS;

        // 创建请求体 JSON 数组
        JsonArray jsonArray = new JsonArray();
        JsonObject envObj = new JsonObject();
        envObj.addProperty("name", name);
        envObj.addProperty("value", value);
        envObj.addProperty("remarks", remarks);
        jsonArray.add(envObj);

        // 创建请求体
        RequestBody body = RequestBody.create(jsonArray.toString(), MediaType.parse("application/json; charset=utf-8"));

        // 创建请求，添加必要的头信息
        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer " + token).build();

        // 使用 client 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                // 解析 JSON 响应
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 200) {
                    // 获取 data 数组并转换为 QinglongEnv 对象
                    JsonArray dataArray = jsonObject.getAsJsonArray("data");
                    if (!dataArray.isEmpty()) {
                        return gson.fromJson(dataArray.get(0), QinglongEnv.class);
                    } else {
                        throw new IOException("API Error: No data returned");
                    }
                } else {
                    // 处理业务错误
                    throw new IOException("API Error: " + jsonObject.get("code").getAsString() + " - " + (jsonObject.has("msg") ? jsonObject.get("msg").getAsString() : "Unknown error"));
                }
            } else {
                // 处理 HTTP 错误
                throw new IOException("Request failed: " + response.code());
            }
        }
    }

    /**
     * 更新青龙面板环境变量
     * <p>
     * 调用青龙面板API更新已有的环境变量，用于修改北京现代相关的配置信息
     * </p>
     *
     * @param base_url 青龙面板基础URL
     * @param token    青龙面板访问令牌
     * @param id       环境变量ID
     * @param name     环境变量名称
     * @param value    环境变量值
     * @param remarks  环境变量备注
     * @return 更新后的环境变量对象
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static QinglongEnv updateQinglongEnv(String base_url, String token, long id, String name, String value, String remarks) throws IOException {
        // 构建完整 URL
        String url = base_url + API_QINGLONG_ENVS;

        // 创建请求体 JSON 对象
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("id", id);
        jsonBody.addProperty("name", name);
        jsonBody.addProperty("value", value);
        jsonBody.addProperty("remarks", remarks);

        // 创建请求体
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));

        // 创建请求，添加必要的头信息
        Request request = new Request.Builder().url(url).put(body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer " + token).build();

        // 使用 client 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                // 解析 JSON 响应
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 200) {
                    // 获取 data 对象并转换为 QinglongEnv 对象
                    JsonObject dataObject = jsonObject.getAsJsonObject("data");
                    return gson.fromJson(dataObject, QinglongEnv.class);
                } else {
                    // 处理业务错误
                    throw new IOException("API Error: " + jsonObject.get("code").getAsString() + " - " + (jsonObject.has("msg") ? jsonObject.get("msg").getAsString() : "Unknown error"));
                }
            } else {
                // 处理 HTTP 错误
                throw new IOException("Request failed: " + response.code());
            }
        }
    }

    /**
     * 删除青龙面板环境变量
     * <p>
     * 调用青龙面板API删除指定ID的环境变量，用于移除北京现代相关的配置信息
     * </p>
     *
     * @param base_url 青龙面板基础URL
     * @param token    青龙面板访问令牌
     * @param id       要删除的环境变量ID
     * @return 是否删除成功
     * @throws IOException 如果网络请求失败或服务器返回错误
     */
    public static boolean deleteQinglongEnv(String base_url, String token, long id) throws IOException {
        // 构建完整 URL
        String url = base_url + API_QINGLONG_ENVS;

        // 创建请求体 JSON 数组，只包含单个 id
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(id);

        // 创建请求体
        RequestBody body = RequestBody.create(jsonArray.toString(), MediaType.parse("application/json; charset=utf-8"));

        // 创建请求，添加必要的头信息
        Request request = new Request.Builder().url(url).delete(body).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader("Authorization", "Bearer " + token).build();

        // 使用 client 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();

                // 解析 JSON 响应
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                return jsonObject.get("code").getAsInt() == 200;
            } else {
                // 处理 HTTP 错误
                throw new IOException("Request failed: " + response.code());
            }
        }
    }
}
