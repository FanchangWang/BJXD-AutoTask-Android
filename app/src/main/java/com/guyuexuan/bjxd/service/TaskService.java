package com.guyuexuan.bjxd.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.guyuexuan.bjxd.AnswerActivity;
import com.guyuexuan.bjxd.R;
import com.guyuexuan.bjxd.TaskActivity;
import com.guyuexuan.bjxd.TaskLogActivity;
import com.guyuexuan.bjxd.task.TaskRunner;
import com.guyuexuan.bjxd.util.ApiUtil;
import com.guyuexuan.bjxd.util.StorageUtil;
import com.guyuexuan.bjxd.util.TaskLogStore;

import java.util.List;
import java.util.function.Consumer;

/**
 * 后台任务前台服务。
 * <p>
 * 任务线程不再依附于 Activity，App 退到后台后依然可以执行；执行期间用常驻通知显示进度，
 * 结束后发一条可点击的通知，点开即可查看完整日志。持 PARTIAL_WAKE_LOCK 防止 CPU 休眠。
 * </p>
 */
public class TaskService extends Service {
    public static final String ACTION_START = "com.guyuexuan.bjxd.action.TASK_START";
    public static final String ACTION_STOP = "com.guyuexuan.bjxd.action.TASK_STOP";
    public static final String ACTION_ANSWER = "com.guyuexuan.bjxd.action.TASK_ANSWER";
    public static final String EXTRA_ANSWER = "answer";

    private static final String CHANNEL_PROGRESS = "task_progress";
    private static final String CHANNEL_RESULT = "task_result";
    private static final String CHANNEL_ANSWER = "task_answer";
    private static final int NOTIF_PROGRESS = 1001;
    private static final int NOTIF_RESULT = 1002;
    private static final int NOTIF_ANSWER = 1003;
    private static final long ANSWER_TIMEOUT_MS = 2 * 60 * 1000L;

    private static volatile TaskService instance;
    private final Object answerLock = new Object();
    // 这些字段必须在 serviceLogger 之前声明：lambda 初始化器引用了它们，Java 禁止前向引用
    private NotificationManager notificationManager;
    private TaskLogStore logStore;
    private volatile TaskRunner runner;
    private PowerManager.WakeLock wakeLock;
    private volatile String pendingAnswer = null;
    private volatile long lastNotificationUpdate = 0;
    private final Consumer<String> serviceLogger = line -> {
        if (logStore != null) {
            logStore.log(line);
        }
        long now = System.currentTimeMillis();
        if (now - lastNotificationUpdate > 1500) {
            lastNotificationUpdate = now;
            showProgressNotification(line);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, TaskService.class).setAction(ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stop(Context context) {
        TaskService service = instance;
        if (service != null) {
            service.stopCurrentTask();
            return;
        }
        try {
            context.startService(new Intent(context, TaskService.class).setAction(ACTION_STOP));
        } catch (Exception ignored) {
            // 服务已不在运行
        }
    }

    public static boolean isRunning() {
        TaskService service = instance;
        return service != null && service.runner != null && service.runner.isTaskRunning();
    }

    /**
     * 通知栏点选答案后由广播转发进来
     */
    public static void submitAnswer(String answer) {
        TaskService service = instance;
        if (service != null) {
            service.deliverAnswer(answer);
        }
    }

    /**
     * 取日志末尾几行作为通知摘要
     */
    private static String buildSummary(String log) {
        String[] lines = log.split("\\r?\\n");
        StringBuilder summary = new StringBuilder();
        int from = Math.max(0, lines.length - 4);
        for (int i = from; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                // String.isEmpty() 自 API 9 可用；CharSequence.isEmpty() 需 API 35，minSdk 25 不能用
                if (!summary.toString().isEmpty()) {
                    summary.append(' ');
                }
                summary.append(line);
            }
        }
        String text = summary.toString();
        return text.isEmpty() ? "点击查看完整任务日志" : text;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        logStore = TaskLogStore.getInstance(this);
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopCurrentTask();
            stopSelf();
        } else if (ACTION_START.equals(action) && (runner == null || !runner.isTaskRunning())) {
            startTask();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int elapsedMillis) {
        // targetSdk 35+ 的 dataSync 前台服务有 6 小时上限；到时优雅收尾而不是被系统强杀
        serviceLogger.accept("⚠️ 前台服务超时，正在保存状态并结束任务");
        stopCurrentTask();
        onTaskFinished();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTask() {
        logStore.startNewSession();
        showProgressNotification("任务已在后台启动…");
        acquireWakeLock();
        runner = new TaskRunner(
                StorageUtil.getInstance(this),
                serviceLogger,
                this::onTaskFinished,
                this::requestManualAnswer);
        runner.start();
    }

    void stopCurrentTask() {
        if (runner != null) {
            serviceLogger.accept("🛑 收到停止指令，正在结束任务…");
            runner.stopTask();
            ApiUtil.cancelAllCalls();
        }
        // 如果正卡在手动答题等待上，立即唤醒
        synchronized (answerLock) {
            answerLock.notifyAll();
        }
    }

    private void deliverAnswer(String answer) {
        synchronized (answerLock) {
            pendingAnswer = answer;
            answerLock.notifyAll();
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ANSWER);
        }
    }

    /**
     * 手动答题：发一条带 A/B/C/D 按钮的通知，等待用户点选（超时则返回 null）
     */
    private String requestManualAnswer(String question, String optionsText,
                                       List<String> availableOptionLetters) throws InterruptedException {
        showAnswerNotification(question, optionsText, availableOptionLetters);
        synchronized (answerLock) {
            // 不在进入时清空：用户可能在通知发出后、线程进入等待前就已点击答案
            long deadline = android.os.SystemClock.elapsedRealtime() + ANSWER_TIMEOUT_MS;
            try {
                while (pendingAnswer == null) {
                    long remaining = deadline - android.os.SystemClock.elapsedRealtime();
                    if (remaining <= 0L) break;
                    answerLock.wait(remaining);
                }
            } catch (InterruptedException e) {
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIF_ANSWER);
                }
                throw e;
            }
            String answer = pendingAnswer;
            pendingAnswer = null;
            if (answer == null) {
                serviceLogger.accept("⏰ 等待手动答题超时（2 分钟），自动改用 AI / 随机答案");
            }
            return answer;
        }
    }

    private void onTaskFinished() {
        runner = null;
        releaseWakeLock();
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ANSWER);
        }
        logStore.setRunning(false);
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        postResultNotification(buildSummary(logStore.getLogText()));
        stopSelf();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) {
            return;
        }
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":task");
        wakeLock.setReferenceCounted(false);
        // 兜底 2 小时自动释放，避免异常情况下永久持锁
        wakeLock.acquire(2 * 60 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }

    // ------------------------------------------------------------------
    // 通知
    // ------------------------------------------------------------------

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return;
        }
        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_PROGRESS, "任务进度", NotificationManager.IMPORTANCE_LOW));
        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_RESULT, "任务结果", NotificationManager.IMPORTANCE_DEFAULT));
        notificationManager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ANSWER, "手动答题", NotificationManager.IMPORTANCE_HIGH));
    }

    private void showProgressNotification(String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, TaskActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, TaskActionReceiver.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 99, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_PROGRESS)
                .setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle("北京现代任务进行中")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_delete, "停止", stopPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_PROGRESS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_PROGRESS, notification);
        }
    }

    private void postResultNotification(String summary) {
        if (notificationManager == null) {
            return;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 1,
                new Intent(this, TaskLogActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_RESULT)
                .setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle("✅ 北京现代任务已完成")
                .setContentText(summary)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(summary))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        notificationManager.notify(NOTIF_RESULT, notification);
    }

    private void showAnswerNotification(String question, String optionsText,
                                        List<String> availableOptionLetters) {
        if (notificationManager == null) {
            return;
        }
        String firstLine = question.split("\\r?\\n")[0];

        // 兜底：点击通知正文直接打开答题框，不依赖 ROM 对通知 Action 的支持
        // （MIUI 等国产 ROM 只在展开的大视图显示 Actions，且最多 3 个）
        Intent answerActivityIntent = new Intent(this, AnswerActivity.class)
                .putExtra(AnswerActivity.EXTRA_QUESTION, question)
                .putExtra(AnswerActivity.EXTRA_OPTIONS_TEXT, optionsText)
                .putExtra(AnswerActivity.EXTRA_LETTERS, availableOptionLetters.toArray(new String[0]));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 200, answerActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ANSWER)
                .setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle("🤔 请选择今日答题答案")
                .setContentText(firstLine + "（点此选择）")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(question + optionsText))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                        new NotificationCompat.Builder(this, CHANNEL_ANSWER)
                                .setSmallIcon(R.drawable.ic_stat_task)
                                .setContentTitle("北京现代答题提醒")
                                .setContentText("请解锁设备查看题目")
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                                .build()
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        for (String letter : availableOptionLetters) {
            Intent answerIntent = new Intent(this, TaskActionReceiver.class)
                    .setAction(ACTION_ANSWER)
                    .putExtra(EXTRA_ANSWER, letter);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, letter.charAt(0), answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_menu_send, letter, pendingIntent);
        }

        notificationManager.notify(NOTIF_ANSWER, builder.build());
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        if (runner != null) {
            runner.stopTask();
        }
        instance = null;
        super.onDestroy();
    }
}
