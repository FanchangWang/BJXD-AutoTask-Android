package com.guyuexuan.bjxd.util;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
    private static final OkHttpClient aiClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)      // 连接超时可以适当放宽
            .readTimeout(5, TimeUnit.MINUTES)        // 读取超时设置为3分钟，等待AI响应
            .writeTimeout(5, TimeUnit.MINUTES)       // 写入超时也设置为3分钟
            .retryOnConnectionFailure(true).build();

    private static final Gson gson = new Gson();

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
                String json = response.body().string();
                System.out.println("getUserInfo API Response: " + json);

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                if (jsonObject.get("code").getAsInt() == 0) {
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    // 手动添加 token，因为 data 中没有这个字段，而 User 类需要它
                    data.addProperty("token", token);
                    return User.fromJson(data);
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
                System.out.println("getScore API Response: " + json);

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
                System.out.println("getTaskStatus API Response: " + json);

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
                System.out.println("getSignInfo API Response: " + json);

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
                System.out.println("submitSign API Response: " + json);

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
                System.out.println("getArticleList API Response: " + json);

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
                System.out.println("viewArticle API Response: " + json);

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
                System.out.println("submitArticleScore API Response: " + json);

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
                System.out.println("getQuestionInfo API Response: " + json);

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
                System.out.println("submitQuestionAnswer API Response: " + json);

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
                System.out.println("askAI API Response: " + json);

                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                return jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }
}
