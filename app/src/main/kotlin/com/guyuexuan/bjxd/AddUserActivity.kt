package com.guyuexuan.bjxd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.guyuexuan.bjxd.model.User
import com.guyuexuan.bjxd.ui.AddUserScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.ApiUtil
import com.guyuexuan.bjxd.util.StorageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddUserActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var storageUtil: StorageUtil
    private var mode: String = MainActivity.MODE_LOGIN
    private var busy by mutableStateOf(false)
    private var error by mutableStateOf("")
    private var detectedUser by mutableStateOf<User?>(null)
    private var detectedIsUpdate by mutableStateOf(false)
    private var detectedToken: String? = null
    private var lastCheckedToken: String? = null
    private var inFlightToken: String? = null
    private var lastFailureAt = 0L
    private var failedToken: String? = null
    private var ignoredToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storageUtil = StorageUtil.getInstance(this)
        mode = intent.getStringExtra(MainActivity.EXTRA_MODE) ?: MainActivity.MODE_LOGIN

        setContent {
            AppTheme {
                AddUserScreen(
                    mode = mode,
                    busy = busy,
                    error = error,
                    detectedUser = detectedUser,
                    detectedIsUpdate = detectedIsUpdate,
                    onBack = { finish() },
                    onTokenDetected = { token -> onDetectedToken(token) },
                    onContinueAdd = {
                        // 当前账号已自动添加并忽略；关闭弹窗继续检测下一个登录账号
                        detectedUser = null
                        detectedToken = null
                    },
                    onBackToHome = { finish() },
                    onTokenSubmit = { token ->
                        if (token.isBlank()) {
                            error = "请先粘贴 Token"
                        } else {
                            addOrUpdateUser(token)
                        }
                    },
                )
            }
        }
    }

    /**
     * WebView localStorage 只有 token（JWT），没有手机号；但 JWT payload 里有 user_hid，
     * 与本地 User.hid 对应，可以直接本地匹配，省掉接口调用。
     * - token 与本地账号相同 → 忽略；
     * - user_hid 匹配到本地账号但 token 不同 → 用新 token 更新本地账号；
     * - 本地找不到 → 调接口获取账号信息后自动添加。
     */
    private fun onDetectedToken(token: String) {
        if (token == lastCheckedToken || token == ignoredToken || token == inFlightToken) return
        // 失败后 5 秒内不再重试同一 token，避免 1.5 秒轮询形成请求风暴
        if (token == failedToken && System.currentTimeMillis() - lastFailureAt < 5_000L) return
        inFlightToken = token
        busy = true
        error = ""
        scope.launch {
            try {
                val hid = User.parseJwtHid(token)
                val localUser = if (hid.isNotBlank()) {
                    storageUtil.userList.firstOrNull { it.hid == hid }
                } else {
                    null
                }

                if (localUser != null) {
                    if (localUser.token == token) {
                        ignoredToken = token
                        lastCheckedToken = token
                        return@launch
                    }
                    val updated = User(token, localUser.nickname, localUser.phone, localUser.hid)
                    finishWithUser(updated, isUpdate = true)
                } else {
                    val user = withContext(Dispatchers.IO) { ApiUtil.getUserInfo(token) }
                    val existing =
                        storageUtil.userList.firstOrNull { it.hid == user.hid || it.phone == user.phone }
                    if (existing != null) {
                        if (existing.token == token) {
                            ignoredToken = token
                            lastCheckedToken = token
                            return@launch
                        }
                        val updated = User(token, user.nickname, user.phone, user.hid)
                        finishWithUser(updated, isUpdate = true)
                    } else {
                        finishWithUser(user, isUpdate = false)
                    }
                }
            } catch (e: Exception) {
                failedToken = token
                lastFailureAt = System.currentTimeMillis()
                error = when {
                    e.message?.contains("403") == true -> "检测到的 Token 已过期，请重新登录"
                    else -> "获取账号信息失败：${e.message ?: "未知错误"}"
                }
            } finally {
                inFlightToken = null
                busy = false
            }
        }
    }

    private fun finishWithUser(user: User, isUpdate: Boolean) {
        user.setAddedTime(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date()))
        val position = storageUtil.saveUser(user)
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(MainActivity.EXTRA_USER, user)
                putExtra(MainActivity.EXTRA_POSITION, position)
            }
        )
        ignoredToken = user.token
        lastCheckedToken = user.token
        detectedToken = user.token
        detectedIsUpdate = isUpdate
        detectedUser = user
    }

    private fun addOrUpdateUser(token: String) {
        busy = true
        error = ""
        scope.launch {
            try {
                val user = withContext(Dispatchers.IO) { ApiUtil.getUserInfo(token) }
                // 显式记录本次添加/更新的时间
                user.setAddedTime(
                    SimpleDateFormat(
                        "MM-dd HH:mm",
                        Locale.getDefault()
                    ).format(Date())
                )
                val position = storageUtil.saveUser(user)
                val result = Intent().apply {
                    putExtra(MainActivity.EXTRA_USER, user)
                    putExtra(MainActivity.EXTRA_POSITION, position)
                }
                setResult(RESULT_OK, result)
                finish()
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("403") == true -> "Token 已过期，请重新获取后添加"
                    else -> "获取账号失败：${e.message ?: "未知错误"}"
                }
            } finally {
                busy = false
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
