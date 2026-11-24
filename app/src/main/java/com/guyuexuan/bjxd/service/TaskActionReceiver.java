package com.guyuexuan.bjxd.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 接收通知栏上的答案选择与停止指令
 */
public class TaskActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        if (TaskService.ACTION_ANSWER.equals(intent.getAction())) {
            String answer = intent.getStringExtra(TaskService.EXTRA_ANSWER);
            if (answer != null) {
                TaskService.submitAnswer(answer);
            }
        } else if (TaskService.ACTION_STOP.equals(intent.getAction())) {
            TaskService.stop(context);
        }
    }
}
