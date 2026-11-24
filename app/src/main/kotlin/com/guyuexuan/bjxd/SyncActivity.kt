package com.guyuexuan.bjxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.guyuexuan.bjxd.sync.QinglongSync
import com.guyuexuan.bjxd.ui.SyncScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.StorageUtil
import com.guyuexuan.bjxd.util.SyncStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class SyncActivity : ComponentActivity() {
    // 作业不随 onDestroy 取消（同步必须跑完），但 UI 状态托管在 SyncStateStore，旋转/重建不丢
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var storageUtil: StorageUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storageUtil = StorageUtil.getInstance(this)

        setContent {
            AppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val uiScope = rememberCoroutineScope()
                val hint = {
                    uiScope.launch {
                        snackbarHostState.showSnackbar(
                            "正在同步账号，请等待完成后返回",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }

                BackHandler(enabled = SyncStateStore.isRunning) { hint() }

                SyncScreen(
                    log = SyncStateStore.logText,
                    running = SyncStateStore.isRunning,
                    snackbarHostState = snackbarHostState,
                    onBack = { if (SyncStateStore.isRunning) hint() else finish() },
                )

                if (SyncStateStore.showFailureDialog) {
                    AlertDialog(
                        onDismissRequest = { SyncStateStore.dismissFailureDialog() },
                        title = { Text("部分账号未同步到青龙") },
                        text = {
                            Text(
                                "有 ${SyncStateStore.failedNames.size} 个账号的云端操作失败（删除或写入，已各重试 3 次）：" +
                                        SyncStateStore.failedNames.joinToString("、") +
                                        "。本地账号已保存，不会受影响。" +
                                        "请检查青龙面板地址、密钥配置以及青龙服务状态；" +
                                        "确认无误后，可从首页再次点击「同步到青龙」重试。"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { SyncStateStore.dismissFailureDialog() }) { Text("知道了") }
                        },
                    )
                }
            }
        }

        if (savedInstanceState != null) return
        if (!storageUtil.checkQinglongSettings()) {
            SyncStateStore.setNotConfigured()
        } else {
            startSync()
        }
    }

    private fun startSync() {
        if (SyncStateStore.isRunning || !SYNC_IN_PROGRESS.compareAndSet(false, true)) return
        SyncStateStore.begin()

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    QinglongSync(
                        storageUtil = storageUtil,
                        logger = { line -> runOnUiThread { SyncStateStore.append(line) } },
                    ).run()
                }
                SyncStateStore.finish(result.failed)
            } catch (e: Exception) {
                SyncStateStore.append("同步失败：${e.message ?: "未知错误"}")
                SyncStateStore.finish(emptyList())
            } finally {
                SYNC_IN_PROGRESS.set(false)
            }
        }
    }

    companion object {
        private val SYNC_IN_PROGRESS = AtomicBoolean(false)
    }
}

