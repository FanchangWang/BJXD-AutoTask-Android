package com.guyuexuan.bjxd.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.ApiUtil;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动任务执行器。从 Activity 中剥离出来，运行在后台 Service 的线程里，
 * 因此 App 退到后台后任务依然可以继续执行。
 */
public class TaskRunner extends Thread {
    /**
     * 仅用于测试账号失效（403）提示：临时给 token 末尾加错误字符。测完改回 false
     */
    private static final boolean DEBUG_INVALID_TOKEN = false;
    private static final String DEBUG_TOKEN_SUFFIX = "_BAD";
    private static final String[] BACKUP_HIDS = {
            "a6688ec1a9ee429fa7b68d50e0c92b1f",
            "bb8cd2e44c7b45eeb8cc5f7fa71c3322",
            "5f640c50061b400c91be326c8fe0accd",
            "55a5d82dacd9417483ae369de9d9b82d"
    };
    private final List<User> userList;
    private final StorageUtil storageUtil;
    private final Consumer<String> logger;
    private final Runnable onComplete;
    private final ManualAnswerProvider manualAnswerProvider;
    private final List<String> wrongAnswers = new ArrayList<>();
    private final Map<String, String> searchCache = new HashMap<>();
    private final Random random = new Random();
    private volatile boolean running = true;
    private int currentUserIndex = 0;
    private String historicalCorrectAnswer = null;

    public TaskRunner(StorageUtil storageUtil,
                      Consumer<String> logger,
                      Runnable onComplete,
                      ManualAnswerProvider manualAnswerProvider) {
        super("BJXD-Task");
        this.userList = storageUtil.getUserList();
        this.storageUtil = storageUtil;
        this.logger = logger;
        this.onComplete = onComplete;
        this.manualAnswerProvider = manualAnswerProvider;
    }

    /**
     * 用题干（去掉选项）作为搜索关键词
     */
    private static String buildSearchQuery(String question) {
        for (String line : question.split("\\r?\\n")) {
            String stem = line.trim();
            if (!stem.isEmpty()) {
                return stem.length() > 120 ? stem.substring(0, 120) : stem;
            }
        }
        return "";
    }

    /**
     * 与 Python 版保持一致：把搜索资料拼进 user prompt
     */
    private static String buildAiPrompt(String questionWithOptions, String material) {
        if (material == null || material.isEmpty()) {
            return questionWithOptions;
        }
        return "以下是联网搜索到的资料，可能包含本题相关信息，仅供参考；"
                + "若与题目无关请忽略，必要时结合你自己的知识判断。\n"
                + "===== 资料开始 =====\n"
                + material + "\n"
                + "===== 资料结束 =====\n\n"
                + "题目与选项：\n"
                + questionWithOptions + "\n\n"
                + "请只输出正确答案对应的那个英文字母。";
    }

    @Override
    public void run() {
        try {
            if (userList.isEmpty()) {
                logger.accept("🚨 用户列表为空，请先添加账号！");
            } else {
                logger.accept(String.format(Locale.getDefault(), "共有 %d 个用户任务待执行", userList.size()));

                logger.accept("\nRUN: 设置分享用户ID");
                setupShareUserHids();

                logger.accept(String.format(Locale.getDefault(), "\nRUN: 执行任务, 共 %d 个账号", userList.size()));
                for (User user : userList) {
                    try {
                        checkShouldStop();
                        currentUserIndex++;
                        if (currentUserIndex > 1) {
                            logger.accept("进行下一个账号, 等待 5-10 秒...");
                            TimeUnit.MILLISECONDS.sleep(5000 + random.nextInt(5000));
                        }

                        checkShouldStop();
                        logger.accept(String.format(Locale.getDefault(), "\n======> 第 %d 个账号", currentUserIndex));
                        executeUserTask(user);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw e;
                    } catch (Exception e) {
                        logApiError(user, "执行任务", e);
                    }
                }

                logger.accept(String.format(Locale.getDefault(), "\nRUN: 积分详情, 共 %d 个账号", userList.size()));
                logger.accept("\n============ 积分详情 ============");
                for (int i = 0; i < userList.size(); i++) {
                    User user = userList.get(i);
                    if (i > 0) {
                        logger.accept("\n进行下一个账号, 等待 5-10 秒...");
                        TimeUnit.MILLISECONDS.sleep(5000 + random.nextInt(5000));
                    }

                    logger.accept(String.format(Locale.getDefault(), "\n======== ▷ 第 %d 个账号 ◁ ========", i + 1));
                    logger.accept(String.format("👻 用户名: %s | 手机号: %s", user.getNickname(), user.getMaskedPhone()));
                    getScoreDetails(user);
                }
            }
        } catch (InterruptedException e) {
            logger.accept("🚨 任务被中断: " + e.getMessage());
        } finally {
            logger.accept("✅ 任务已结束，结果已通知。");
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private String tokenOf(User user) {
        return DEBUG_INVALID_TOKEN ? user.getToken() + DEBUG_TOKEN_SUFFIX : user.getToken();
    }

    /**
     * 账号失效（403 / Token 过期）按 Python 版给出统一提示，不再暴露原始状态码
     */
    private void logApiError(User user, String action, Exception e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("403") || msg.contains("过期") || msg.contains("失效"))) {
            logger.accept("⚠️ 账号已失效，请重新获取 token：" + user.getMaskedPhone());
        } else {
            logger.accept(action + "失败：" + (msg == null ? "未知错误" : msg));
        }
    }

    private void checkShouldStop() throws InterruptedException {
        if (!running || isInterrupted()) {
            logger.accept("🚨 检测到停止命令，停止执行任务");
            throw new InterruptedException("Task stopped");
        }
    }

    public void stopTask() {
        running = false;
        interrupt();
    }

    // ------------------------------------------------------------------
    // 账号 / 分享 hid
    // ------------------------------------------------------------------

    public boolean isTaskRunning() {
        return running && isAlive();
    }

    private void setupShareUserHids() {
        if (userList.size() <= 1) {
            for (User user : userList) {
                user.setShareUserHid(getBackupShareHid(user.getHid()));
            }
            return;
        }

        for (int i = 0; i < userList.size(); i++) {
            User currentUser = userList.get(i);
            int prevIndex = (i - 1 >= 0) ? i - 1 : userList.size() - 1;
            User prevUser = userList.get(prevIndex);

            if (!prevUser.getHid().equals(currentUser.getHid())) {
                currentUser.setShareUserHid(prevUser.getHid());
            } else {
                currentUser.setShareUserHid(getBackupShareHid(currentUser.getHid()));
            }
        }
    }

    // ------------------------------------------------------------------
    // 任务分发
    // ------------------------------------------------------------------

    private String getBackupShareHid(String currentHid) {
        List<String> availableHids = new ArrayList<>();
        for (String hid : BACKUP_HIDS) {
            if (!hid.equals(currentHid)) {
                availableHids.add(hid);
            }
        }
        if (availableHids.isEmpty()) {
            return BACKUP_HIDS[0];
        }
        return availableHids.get(random.nextInt(availableHids.size()));
    }

    // ------------------------------------------------------------------
    // 签到
    // ------------------------------------------------------------------

    private void executeUserTask(User user) throws InterruptedException {
        logger.accept(String.format("👻 用户名: %s  📱手机号: %s", user.getNickname(), user.getMaskedPhone()));
        logger.accept(String.format("🆔 用户hid: %s", user.getHid()));
        logger.accept(String.format("🆔 分享hid: %s", user.getShareUserHid()));

        try {
            TaskStatus status = ApiUtil.getTaskStatus(tokenOf(user));
            checkShouldStop();

            // 答题放第一个，方便手动答题时尽快处理
            if (!status.isQuestionCompleted()) {
                executeQuestionTask(user);
                TimeUnit.MILLISECONDS.sleep(5000 + random.nextInt(5000));
            } else {
                logger.accept("✅ 答题任务 已完成，跳过");
                if (historicalCorrectAnswer == null) {
                    getAnsweredAnswer(user);
                }
            }

            checkShouldStop();

            if (!status.isSignCompleted()) {
                executeSignTask(user);
                TimeUnit.MILLISECONDS.sleep(5000 + random.nextInt(5000));
            } else {
                logger.accept("✅ 签到任务 已完成，跳过");
            }

            checkShouldStop();

            if (!status.isViewCompleted()) {
                executeViewTask(user);
                TimeUnit.MILLISECONDS.sleep(5000 + random.nextInt(5000));
            } else {
                logger.accept("✅ 浏览文章任务 已完成，跳过");
            }
        } catch (Exception e) {
            logApiError(user, "执行任务", e);
        }
    }

    // ------------------------------------------------------------------
    // 浏览文章
    // ------------------------------------------------------------------

    private void executeSignTask(User user) throws InterruptedException {
        logger.accept("🔍 开始执行签到任务");

        String bestHid = null;
        String bestRewardHash = null;
        int bestScore = 0;
        int maxAttemptCount = 5;

        for (int i = 0; i < maxAttemptCount; i++) {
            checkShouldStop();
            try {
                JsonObject data = ApiUtil.getSignInfo(tokenOf(user));
                String hid = data.get("hid").getAsString();
                String rewardHash = data.get("rewardHash").getAsString();
                int currentBestScore = 0;

                JsonArray list = data.getAsJsonArray("list");
                if (list != null) {
                    for (int j = 0; j < list.size(); j++) {
                        JsonObject item = list.get(j).getAsJsonObject();
                        if (item.get("hid").getAsString().equals(hid)) {
                            currentBestScore = item.get("score").getAsInt();
                            break;
                        }
                    }
                }

                // 没有任何有效参数时也要记录最后一次结果，避免 0 分签到被整个跳过
                if (bestHid == null || currentBestScore >= bestScore) {
                    bestScore = currentBestScore;
                    bestHid = hid;
                    bestRewardHash = rewardHash;
                }
                logger.accept(String.format(Locale.getDefault(), "第 %d 次尝试: score=%d hid=%s rewardHash=%s", i + 1, currentBestScore, hid, rewardHash));
                logger.accept(String.format(Locale.getDefault(), "当前可获得签到积分: %d", bestScore));
            } catch (Exception e) {
                logApiError(user, "获取签到信息", e);
            }

            if (i < maxAttemptCount - 1) {
                logger.accept("继续尝试获取更高签到积分, 延时8-10s");
                TimeUnit.MILLISECONDS.sleep(8000 + random.nextInt(2000));
            } else {
                logger.accept("即将提交签到, 延时3-4s");
                TimeUnit.MILLISECONDS.sleep(3000 + random.nextInt(1000));
            }
        }

        checkShouldStop();

        if (bestHid != null) {
            try {
                ApiUtil.submitSign(tokenOf(user), bestHid, bestRewardHash);
                logger.accept(String.format(Locale.getDefault(), "✅ 签到成功: 积分+%d", bestScore));
            } catch (Exception e) {
                logger.accept("❌ 签到失败: " + e.getMessage());
            }
        } else {
            logger.accept("未找到可用的签到选项");
        }
    }

    // ------------------------------------------------------------------
    // 答题（手动 / AI+百度搜索 / 随机）
    // ------------------------------------------------------------------

    private void executeViewTask(User user) {
        logger.accept("🔍 开始执行浏览文章任务");

        try {
            JsonObject articles = ApiUtil.getArticleList(tokenOf(user));
            JsonArray list = articles.getAsJsonArray("list");
            if (!list.isEmpty()) {
                List<String> articleHids = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    articleHids.add(list.get(i).getAsJsonObject().get("hid").getAsString());
                }
                Collections.shuffle(articleHids);

                int articlesToRead = Math.min(3, articleHids.size());
                logger.accept(String.format(Locale.getDefault(), "需要浏览 %d 篇文章", articlesToRead));

                for (int i = 0; i < articlesToRead; i++) {
                    try {
                        checkShouldStop();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    String articleId = articleHids.get(i);
                    logger.accept(String.format(Locale.getDefault(), "\n浏览第 %d/%d 篇文章: hid=%s", i + 1, articlesToRead, articleId));

                    try {
                        ApiUtil.viewArticle(tokenOf(user), articleId);
                        logger.accept("浏览文章 10-15 秒");
                        TimeUnit.MILLISECONDS.sleep(11000 + random.nextInt(4000));
                    } catch (Exception e) {
                        logApiError(user, "浏览文章", e);
                    }
                }

                try {
                    checkShouldStop();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                try {
                    JsonObject data = ApiUtil.submitArticleScore(tokenOf(user));
                    int score = data.get("score").getAsInt();
                    logger.accept(String.format(Locale.getDefault(), "✅ 浏览文章成功: 积分+%d", score));
                } catch (Exception e) {
                    logApiError(user, "提交文章积分", e);
                }
            } else {
                logger.accept("❌ 没有可浏览的文章");
            }
        } catch (Exception e) {
            logApiError(user, "获取文章列表", e);
        }
    }

    private String getQuestionAnswer(String question, String optionsText, List<String> availableOptionLetters)
            throws InterruptedException {
        // 历史答案必须属于当前题目的选项集（题目可能只有 2-3 个选项）
        if (historicalCorrectAnswer != null && availableOptionLetters.contains(historicalCorrectAnswer)) {
            logger.accept("使用历史正确答案: " + historicalCorrectAnswer);
            return historicalCorrectAnswer;
        }

        if (storageUtil.isManualAnswer() && manualAnswerProvider != null && !availableOptionLetters.isEmpty()) {
            String answer = manualAnswerProvider.requestAnswer(question, optionsText, availableOptionLetters);
            if (answer != null && availableOptionLetters.contains(answer)) {
                logger.accept("用户选择答案: " + answer);
                return answer;
            }
            logger.accept("⚠️ 未收到有效的手动答案，改用 AI / 随机答案");
        }

        if (storageUtil.checkAiSettings()) {
            logger.accept("使用 AI 查询答案……");
            try {
                // 1) 百度联网搜索题干，失败时自动降级为空
                String searchMaterial = "";
                if (storageUtil.isSearchEnabled() && !storageUtil.getSearchApiKey().isEmpty()) {
                    String query = buildSearchQuery(question);
                    logger.accept("🔍 联网搜索题干: " + query);
                    if (searchCache.containsKey(query)) {
                        logger.accept("💾 复用已缓存的搜索资料");
                        searchMaterial = searchCache.get(query);
                    } else {
                        searchMaterial = ApiUtil.searchBaidu(
                                storageUtil.getSearchApiKey(),
                                storageUtil.getSearchResultCount(),
                                storageUtil.getSearchTimeout(),
                                query,
                                logger);
                        if (!searchMaterial.isEmpty()) {
                            searchCache.put(query, searchMaterial);
                        }
                    }
                } else {
                    logger.accept("🔎 未启用百度联网搜索，仅用题目询问 AI");
                }

                // 2) 拼装 prompt：资料 + 题目 + 选项
                String prompt = buildAiPrompt(question + optionsText, searchMaterial);
                String aiResult = ApiUtil.askAI(
                        storageUtil.getAiApiKey(),
                        storageUtil.getAiRequestUrl(),
                        storageUtil.getAiModel(),
                        storageUtil.getAiRequestParams(),
                        prompt);

                String extractedAnswer = extractAnswer(aiResult, availableOptionLetters, wrongAnswers);
                if (!extractedAnswer.isEmpty()) {
                    logger.accept("使用 AI 答案: " + extractedAnswer);
                    return extractedAnswer;
                }
                logger.accept("AI 回答无效或不在可用选项中: " + aiResult);
            } catch (Exception e) {
                logger.accept("AI 请求失败: " + e.getMessage());
            }
        }

        if (!availableOptionLetters.isEmpty()) {
            String answer = availableOptionLetters.get(random.nextInt(availableOptionLetters.size()));
            logger.accept("从可用选项中随机选择答案: " + answer);
            return answer;
        }

        String[] options = {"A", "B", "C", "D"};
        String answer = options[random.nextInt(options.length)];
        logger.accept("没有可用选项，使用完全随机答案: " + answer);
        return answer;
    }

    /**
     * 从 AI 回复中提炼 A/B/C/D，并排除已知错误答案
     */
    private String extractAnswer(String raw, List<String> available, List<String> wrong) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.toUpperCase(Locale.ROOT)
                .replace('Ａ', 'A')
                .replace('Ｂ', 'B')
                .replace('Ｃ', 'C')
                .replace('Ｄ', 'D')
                .trim();

        Set<String> valid = new HashSet<>(available.isEmpty() ? Arrays.asList("A", "B", "C", "D") : available);
        valid.removeAll(wrong);
        if (valid.isEmpty()) {
            valid = new HashSet<>(available.isEmpty() ? Arrays.asList("A", "B", "C", "D") : available);
        }
        if (valid.isEmpty()) {
            return "";
        }

        if (valid.contains(normalized)) {
            return normalized;
        }

        Matcher matcher = Pattern.compile("(?<![A-Z])[ABCD](?![A-Z])").matcher(normalized);
        while (matcher.find()) {
            if (valid.contains(matcher.group())) {
                return matcher.group();
            }
        }

        for (int i = 0; i < normalized.length(); i++) {
            String letter = String.valueOf(normalized.charAt(i));
            if (valid.contains(letter)) {
                return letter;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void executeQuestionTask(User user) {
        logger.accept("🔍 开始执行答题任务");

        try {
            JsonObject data = ApiUtil.getQuestionInfo(tokenOf(user));
            JsonObject questionInfo = data.getAsJsonObject("question_info");

            int state = data.get("state").getAsInt();
            if (state == 3) {
                logger.accept("今日已答题但回答错误，当前无人帮助答题，跳过");
                return;
            }
            if (state != 1) {
                if (data.has("answer")) {
                    String answerText = data.get("answer").getAsString();
                    if (answerText.matches("[A-D].*")) {
                        String answer = answerText.substring(0, 1);
                        historicalCorrectAnswer = answer;
                        logger.accept(String.format("今日已答题，跳过，答案：%s", answer));
                        return;
                    }
                }
                logger.accept("今日已答题，但未获取到答案，跳过");
                return;
            }

            String questionId = questionInfo.get("questions_hid").getAsString();
            String question = questionInfo.get("content").getAsString();
            JsonArray options = questionInfo.getAsJsonArray("option");

            StringBuilder optionsText = new StringBuilder("\n");
            List<String> availableOptionLetters = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                JsonObject option = options.get(i).getAsJsonObject();
                String optionLetter = option.get("option").getAsString();
                String optionContent = option.get("option_content").getAsString();
                if (wrongAnswers.contains(optionLetter)) {
                    logger.accept(String.format("排除错误选项: %s.%s", optionLetter, optionContent));
                } else {
                    availableOptionLetters.add(optionLetter);
                    optionsText.append(String.format("%s. %s\n", optionLetter, optionContent));
                }
            }

            logger.accept("题目详情:\n" + question + optionsText);

            String answer;
            if (availableOptionLetters.size() == 1) {
                answer = availableOptionLetters.get(0);
                logger.accept("只剩下一个选项，自动选择答案: " + answer);
            } else {
                checkShouldStop();
                answer = getQuestionAnswer(question, optionsText.toString(), availableOptionLetters);
            }

            checkShouldStop();

            JsonObject result = ApiUtil.submitQuestionAnswer(tokenOf(user), questionId, answer, user.getShareUserHid());
            int submitAnswerState = result.get("state").getAsInt();
            if (submitAnswerState == 3) {
                wrongAnswers.add(answer);
                if (Objects.equals(historicalCorrectAnswer, answer)) {
                    historicalCorrectAnswer = null;
                }
                logger.accept("❌ 答题错误");
            } else if (submitAnswerState == 2) {
                historicalCorrectAnswer = answer;
                int score = result.get("answer_score").getAsInt();
                logger.accept(String.format(Locale.getDefault(), "✅ 答题正确 | 积分+%d", score));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logApiError(user, "答题", e);
        }
    }

    private void getAnsweredAnswer(User user) {
        try {
            JsonObject data = ApiUtil.getQuestionInfo(tokenOf(user));
            if (data.has("answer")) {
                String answerText = data.get("answer").getAsString();
                if (answerText.matches("[A-D].*")) {
                    String answer = answerText.substring(0, 1);
                    historicalCorrectAnswer = answer;
                    logger.accept(String.format("从已答题账号获取到答案：%s", answer));
                    return;
                }
            }
            logger.accept("从已答题账号获取答案失败");
        } catch (Exception e) {
            logApiError(user, "获取已答题信息", e);
        }
    }

    private void getScoreDetails(User user) throws InterruptedException {
        checkShouldStop();
        try {
            JsonObject data = ApiUtil.getScore(tokenOf(user));
            int totalScore = data.get("score").getAsInt();

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            JsonArray records = data.getAsJsonObject("points_record").getAsJsonArray("list");

            List<JsonObject> todayRecords = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                JsonObject record = records.get(i).getAsJsonObject();
                if (record.get("created_at").getAsString().startsWith(today)) {
                    todayRecords.add(record);
                }
            }

            int todayScore = 0;
            for (JsonObject record : todayRecords) {
                todayScore += Integer.parseInt(record.get("score_str").getAsString().replace("+", ""));
            }

            String todayScoreStr = todayScore > 0 ? "+" + todayScore : String.valueOf(todayScore);
            logger.accept(String.format(Locale.getDefault(), "🎉 总积分: %d | 今日积分变动: %s", totalScore, todayScoreStr));

            int currentMonth = data.has("current_month") && !data.get("current_month").isJsonNull()
                    ? data.get("current_month").getAsInt() : 0;
            int halfYear = data.has("half_year") && !data.get("half_year").isJsonNull()
                    ? data.get("half_year").getAsInt() : 0;
            logger.accept(String.format(Locale.getDefault(),
                    "⏳ 即将到期积分：本月 %d，半年内 %d", currentMonth, halfYear));

            if (!todayRecords.isEmpty()) {
                logger.accept("今日积分记录：");
                for (JsonObject record : todayRecords) {
                    logger.accept(String.format("%s %s %s",
                            record.get("created_at").getAsString(),
                            record.get("desc").getAsString(),
                            record.get("score_str").getAsString()));
                }
            } else {
                logger.accept("今日暂无积分变动");
            }
        } catch (Exception e) {
            logApiError(user, "获取积分信息", e);
        }
    }

    /**
     * 手动答题提供者：由 Service 弹通知让用户在通知栏点选答案，返回 null 表示超时/未选择
     */
    public interface ManualAnswerProvider {
        String requestAnswer(String question, String optionsText, List<String> availableOptionLetters)
                throws InterruptedException;
    }
}
