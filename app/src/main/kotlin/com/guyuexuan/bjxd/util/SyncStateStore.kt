package com.guyuexuan.bjxd.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 青龙同步的进程级状态。旋转重建后新 Activity 仍能读到实时日志与结果，
 * 避免把状态绑在会被销毁的 Activity 实例上。
 */
object SyncStateStore {
    var logText by mutableStateOf("")
        private set

    var isRunning by mutableStateOf(false)
        private set

    var failedNames by mutableStateOf(emptyList<String>())
        private set

    var showFailureDialog by mutableStateOf(false)
        private set

    fun begin() {
        logText = "===== 青龙账号双向同步 =====\n"
        isRunning = true
        failedNames = emptyList()
        showFailureDialog = false
    }

    fun append(line: String) {
        logText += line + "\n"
    }

    fun finish(failed: List<String>) {
        isRunning = false
        failedNames = failed
        showFailureDialog = failed.isNotEmpty()
        append("同步结束，可返回。")
    }

    fun dismissFailureDialog() {
        showFailureDialog = false
    }

    fun setNotConfigured() {
        if (logText.isBlank()) {
            logText = "请先到设置里填写青龙面板地址、CLIENT_ID 和 CLIENT_SECRET。\n"
        }
    }
}

