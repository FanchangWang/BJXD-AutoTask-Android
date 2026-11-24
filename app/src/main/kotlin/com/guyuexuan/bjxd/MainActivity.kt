package com.guyuexuan.bjxd

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.guyuexuan.bjxd.model.User
import com.guyuexuan.bjxd.service.TaskService
import com.guyuexuan.bjxd.ui.MainScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.AppUtils
import com.guyuexuan.bjxd.util.StorageUtil
import com.guyuexuan.bjxd.util.TaskLogStore
import kotlinx.coroutines.launch
import java.util.Collections

class MainActivity : ComponentActivity() {
    private lateinit var storageUtil: StorageUtil
    private val users = mutableStateListOf<User>()

    /** 从存储重新加载，支持在添加账号页连续添加多个账号后返回时列表保持最新 */
    private fun reloadUsers() {
        if (!::storageUtil.isInitialized) return
        users.clear()
        users.addAll(storageUtil.getUserList())
        for (i in 1..DEBUG_FAKE_ACCOUNTS) {
            users.add(
                User(
                    "FAKE_TOKEN_$i",
                    "测试账号 $i",
                    String.format(java.util.Locale.ROOT, "138%08d", i),
                    "fake_hid_$i"
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        reloadUsers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storageUtil = StorageUtil.getInstance(this)
        reloadUsers()

        setContent {
            AppTheme {
                MainContent()
            }
        }
    }

    @Composable
    private fun MainContent() {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var taskRunning by remember { mutableStateOf(TaskService.isRunning()) }
        var lastBackAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
        var showSyncConfirm by remember { mutableStateOf(false) }

        BackHandler(enabled = taskRunning) {
            val now = SystemClock.elapsedRealtime()
            if (lastBackAt > 0L && now - lastBackAt <= BACK_PRESS_WINDOW_MS) {
                moveTaskToBack(true)
                lastBackAt = 0L
            } else {
                lastBackAt = now
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "任务正在后台运行，$BACK_PRESS_WINDOW_S 秒内再按一次返回，才会将应用切到后台，任务继续",
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }

        // 事件驱动：由 TaskLogStore 的运行状态回调刷新，不再每秒轮询
        androidx.compose.runtime.DisposableEffect(Unit) {
            val logStore = TaskLogStore.getInstance(this@MainActivity)
            val stateListener = object : TaskLogStore.TaskLogListener {
                override fun onLog(fullLog: String) = Unit
                override fun onStateChanged(running: Boolean) {
                    taskRunning = running || TaskService.isRunning()
                }
            }
            logStore.addListener(stateListener)
            taskRunning = TaskService.isRunning()
            onDispose { logStore.removeListener(stateListener) }
        }

        val addUserLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    reloadUsers()
                    val data = result.data
                    val name = data?.let {
                        IntentCompat.getSerializableExtra(
                            it,
                            EXTRA_USER,
                            User::class.java
                        )?.nickname
                    }
                    scope.launch { snackbarHostState.showSnackbar("已添加 ${name ?: "账号"}") }
                }
            }

        val notificationPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        MainScreen(
            appTitle = AppUtils.getAppNameWithVersion(this@MainActivity),
            taskRunning = taskRunning,
            showSyncConfirm = showSyncConfirm,
            users = users,
            snackbarHostState = snackbarHostState,
            onAddByLogin = {
                addUserLauncher.launch(
                    Intent(
                        this@MainActivity,
                        AddUserActivity::class.java
                    ).putExtra(EXTRA_MODE, MODE_LOGIN)
                )
            },
            onAddByToken = {
                addUserLauncher.launch(
                    Intent(
                        this@MainActivity,
                        AddUserActivity::class.java
                    ).putExtra(EXTRA_MODE, MODE_TOKEN)
                )
            },
            onOpenSettings = {
                startActivity(
                    Intent(
                        this@MainActivity,
                        ConfigActivity::class.java
                    )
                )
            },
            onOpenSync = {
                if (!storageUtil.checkQinglongSettings()) {
                    startActivity(
                        Intent(this@MainActivity, ConfigActivity::class.java)
                            .putExtra(EXTRA_FOCUS_QINGLONG, true)
                    )
                } else {
                    showSyncConfirm = true
                }
            },
            onSyncConfirmDismiss = { showSyncConfirm = false },
            onSyncConfirm = {
                showSyncConfirm = false
                startActivity(Intent(this@MainActivity, SyncActivity::class.java))
            },
            onStartTask = {
                if (TaskService.isRunning()) {
                    startActivity(Intent(this@MainActivity, TaskActivity::class.java))
                } else if (users.none { !it.token.startsWith("FAKE_") }) {
                    scope.launch { snackbarHostState.showSnackbar("请先添加账号，再开始任务") }
                } else {
                    TaskService.start(this@MainActivity)
                    taskRunning = true
                    startActivity(Intent(this@MainActivity, TaskActivity::class.java))
                }
            },
            onCopyToken = { token ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Token", token))
                scope.launch { snackbarHostState.showSnackbar("Token 已复制") }
            },
            onDeleteUser = { index ->
                users.removeAt(index)
                saveRealUsers(users)
                scope.launch { snackbarHostState.showSnackbar("账号已删除") }
            },
            onMoveUser = { from, to ->
                Collections.swap(users, from, to)
                saveRealUsers(users)
            },
        )
    }

    /** 假账号只用于 UI 测试，不写入存储 */
    private fun saveRealUsers(list: List<User>) {
        storageUtil.saveUserList(list.filter { !it.token.startsWith("FAKE_") })
    }

    companion object {
        /** 首页双击返回切到后台的最大间隔 */
        private const val BACK_PRESS_WINDOW_MS = 2_000L
        private const val BACK_PRESS_WINDOW_S = 2
        const val DEBUG_FAKE_ACCOUNTS = 0
        const val EXTRA_USER = BuildConfig.APPLICATION_ID + ".USER"
        const val EXTRA_POSITION = BuildConfig.APPLICATION_ID + ".POSITION"
        const val EXTRA_MODE = BuildConfig.APPLICATION_ID + ".MODE"
        const val MODE_LOGIN = "login"
        const val MODE_TOKEN = "token"
        const val EXTRA_FOCUS_QINGLONG = BuildConfig.APPLICATION_ID + ".FOCUS_QINGLONG"
    }
}
