package com.guyuexuan.bjxd

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.guyuexuan.bjxd.ui.TaskLogScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.TaskLogStore

class TaskLogActivity : ComponentActivity() {
    private var log by mutableStateOf("")
    private lateinit var listener: TaskLogStore.TaskLogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TaskLogStore.getInstance(this)
        log = store.logText

        listener = object : TaskLogStore.TaskLogListener {
            override fun onLog(fullLog: String) {
                runOnUiThread { log = fullLog }
            }

            override fun onStateChanged(running: Boolean) = Unit
        }
        store.addListener(listener)

        setContent {
            AppTheme {
                TaskLogScreen(
                    log = log,
                    onBack = { finish() },
                    onCopy = {
                        val clipboard =
                            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("任务日志", log))
                        Toast.makeText(this@TaskLogActivity, "日志已复制", Toast.LENGTH_SHORT)
                            .show()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        if (::listener.isInitialized) {
            TaskLogStore.getInstance(this).removeListener(listener)
        }
        super.onDestroy()
    }
}
