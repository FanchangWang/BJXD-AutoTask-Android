package com.guyuexuan.bjxd.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务日志仓库。
 * <p>
 * 任务线程运行在后台 Service 中，Activity 只负责展示，因此日志统一放在这里：
 * - 内存缓冲：供 Activity 实时刷新
 * - 文件持久化：任务结束后点击通知仍能查看完整日志（即使进程被回收后重启）
 * </p>
 */
public final class TaskLogStore {
    private static final String LOG_FILE = "last_task_log.txt";
    private static volatile TaskLogStore instance;
    private final Context appContext;
    private final StringBuilder logBuilder = new StringBuilder();
    private final List<TaskLogListener> listeners = new ArrayList<>();
    private volatile boolean taskRunning = false;

    private TaskLogStore(Context context) {
        appContext = context.getApplicationContext();
        String persisted = readPersisted();
        if (persisted != null) {
            logBuilder.append(persisted);
        }
    }

    public static TaskLogStore getInstance(Context context) {
        if (instance == null) {
            synchronized (TaskLogStore.class) {
                if (instance == null) {
                    instance = new TaskLogStore(context);
                }
            }
        }
        return instance;
    }

    /**
     * 开始新一轮任务：清空旧日志并落盘
     */
    public synchronized void startNewSession() {
        logBuilder.setLength(0);
        logBuilder.append("===== 北京现代任务日志 =====")
                .append('\n')
                .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(new java.util.Date()))
                .append('\n');
        persistLocked();
        setRunning(true);
        notifyLogLocked();
    }

    /**
     * 追加一行日志
     */
    public synchronized void log(String line) {
        logBuilder.append(line).append('\n');
        persistLocked();
        notifyLogLocked();
    }

    public synchronized String getLogText() {
        return logBuilder.toString();
    }

    public boolean isTaskRunning() {
        return taskRunning;
    }

    public void setRunning(boolean running) {
        this.taskRunning = running;
        synchronized (this) {
            for (TaskLogListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onStateChanged(running);
                } catch (Exception ignored) {
                    // 单个监听器异常不影响其他监听器
                }
            }
        }
    }

    public void addListener(TaskLogListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
            listener.onLog(logBuilder.toString());
            listener.onStateChanged(taskRunning);
        }
    }

    public void removeListener(TaskLogListener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    private void notifyLogLocked() {
        String text = logBuilder.toString();
        for (TaskLogListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onLog(text);
            } catch (Exception ignored) {
                // 单个监听器异常不影响其他监听器
            }
        }
    }

    private void persistLocked() {
        File file = new File(appContext.getFilesDir(), LOG_FILE);
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(logBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // 日志落盘失败不影响任务执行
        }
    }

    private String readPersisted() {
        File file = new File(appContext.getFilesDir(), LOG_FILE);
        if (!file.exists()) {
            return null;
        }
        try {
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int read = in.read(bytes);
                if (read <= 0) {
                    return null;
                }
                return new String(bytes, 0, read, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 日志监听：onLog 收到的是完整日志文本，onStateChanged 反映任务是否在运行
     */
    public interface TaskLogListener {
        void onLog(String fullLog);

        void onStateChanged(boolean running);
    }
}
