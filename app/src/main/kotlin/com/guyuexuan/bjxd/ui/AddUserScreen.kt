package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.guyuexuan.bjxd.model.User
import com.guyuexuan.bjxd.ui.theme.spacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    mode: String,
    busy: Boolean,
    error: String,
    detectedUser: User?,
    onBack: () -> Unit,
    onTokenDetected: (String) -> Unit,
    detectedIsUpdate: Boolean,
    onContinueAdd: () -> Unit,
    onBackToHome: () -> Unit,
    onTokenSubmit: (String) -> Unit,
) {
    val isLogin = mode == "login"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isLogin) "手机号登录" else "粘贴 Token") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "add-user-mode",
            ) { targetMode ->
                if (targetMode == "login") {
                    LoginContent(
                        busy = busy,
                        error = error,
                        detectionStopped = detectedUser != null,
                        onTokenDetected = onTokenDetected,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TokenContent(
                        busy = busy,
                        error = error,
                        onSubmit = onTokenSubmit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (isLogin && detectedUser != null) {
        ModalBottomSheet(
            onDismissRequest = onContinueAdd,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Text(
                    text = if (detectedIsUpdate) "账号 Token 已更新" else "账号已自动添加",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = detectedUser.nickname.ifBlank { "未命名账号" },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = detectedUser.maskedPhone,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
                Text(
                    text = if (detectedIsUpdate) {
                        "已用重新登录后的新 Token 更新账号，现在可以直接执行任务。如需添加其他账号，可继续在网页登录。"
                    } else {
                        "已添加到账号列表，现在可以直接执行任务。如需添加其他账号，可继续在网页登录。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("返回首页")
                }
                TextButton(
                    onClick = onContinueAdd,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("继续添加其他账号")
                }
            }
        }
    }
}

@Composable
private fun LoginContent(
    busy: Boolean,
    error: String,
    detectionStopped: Boolean,
    onTokenDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }

    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    LaunchedEffect(detectionStopped) {
        if (detectionStopped) return@LaunchedEffect
        while (true) {
            delay(1500)
            webView.evaluateJavascript("localStorage.getItem('token')") { value ->
                val token = value?.trim()?.trim('"')
                if (!token.isNullOrBlank() && token != "null") {
                    onTokenDetected(token)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "请在下方网页登录北京现代账号。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
        )
        AnimatedVisibility(
            visible = error.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ErrorBanner(error)
        }
        AndroidView(
            factory = {
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.loadUrl("https://bm2-wx.bluemembers.com.cn/browser/login")
                webView
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            CircularProgressIndicator()
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = "正在检测登录状态",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "在上方网页用手机号完成登录后，账号会自动出现在这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TokenContent(
    busy: Boolean,
    error: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var token by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(
            text = "从抓包或其他渠道取得 Token 后粘贴到下方，验证通过即会保存账号。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("账号 Token") },
            supportingText = { Text("Token 有效期约 28 天") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        imageVector = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (reveal) "隐藏 Token" else "显示 Token",
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = error.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            ErrorBanner(error)
        }
        Button(
            onClick = { onSubmit(token.trim()) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("验证并添加")
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun AddUserScreenLightPreview() = PreviewTheme(false) {
    AddUserScreen(
        mode = "token",
        busy = false,
        error = "",
        detectedUser = null,
        onBack = {},
        onTokenDetected = {},
        detectedIsUpdate = false,
        onContinueAdd = {},
        onBackToHome = {},
        onTokenSubmit = {},
    )
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AddUserScreenDarkPreview() = PreviewTheme(true) {
    AddUserScreen(
        mode = "login",
        busy = false,
        error = "",
        detectedUser = com.guyuexuan.bjxd.model.User("token", "李女士", "13800000000", "hid"),
        onBack = {},
        onTokenDetected = {},
        detectedIsUpdate = false,
        onContinueAdd = {},
        onBackToHome = {},
        onTokenSubmit = {},
    )
}
