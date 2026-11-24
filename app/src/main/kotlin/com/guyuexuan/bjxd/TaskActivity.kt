package com.guyuexuan.bjxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.guyuexuan.bjxd.service.TaskService
import com.guyuexuan.bjxd.ui.TaskScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.TaskLogStore

class TaskActivity : ComponentActivity() {
    private var log by mutableStateOf("")
    private var running by mutableStateOf(false)
    private var showExitDialog by mutableStateOf(false)
    private lateinit var logStore: TaskLogStore
    private lateinit var listener: TaskLogStore.TaskLogListener

    private fun isTaskRunning(): Boolean = running || TaskService.isRunning()

    private fun requestBack() {
        if (isTaskRunning()) showExitDialog = true else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logStore = TaskLogStore.getInstance(this)

        listener = object : TaskLogStore.TaskLogListener {
            override fun onLog(fullLog: String) {
                runOnUiThread { log = fullLog }
            }

            override fun onStateChanged(isRunning: Boolean) {
                runOnUiThread { running = isRunning }
            }
        }
        logStore.addListener(listener)
        log = logStore.logText
        running = logStore.isTaskRunning

        setContent {
            AppTheme {
                BackHandler(enabled = isTaskRunning()) { requestBack() }
                TaskScreen(
                    log = log,
                    running = isTaskRunning(),
                    showExitDialog = showExitDialog,
                    onBackRequest = { requestBack() },
                    onDismissExit = { showExitDialog = false },
                    onConfirmExit = {
                        showExitDialog = false
                        finish()
                    },
                    onStop = { TaskService.stop(this@TaskActivity) },
                )
            }
        }

        if (!TaskService.isRunning()) {
            TaskService.start(this)
        }
    }

    override fun onDestroy() {
        logStore.removeListener(listener)
        super.onDestroy()
    }
}
