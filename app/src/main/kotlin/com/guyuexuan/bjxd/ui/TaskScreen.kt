package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.ui.common.LogView
import com.guyuexuan.bjxd.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    log: String,
    running: Boolean,
    showExitDialog: Boolean,
    onBackRequest: () -> Unit,
    onDismissExit: () -> Unit,
    onConfirmExit: () -> Unit,
    onStop: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务") },
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回主页")
                    }
                },
                actions = {
                    if (running) {
                        FilledTonalButton(onClick = onStop) {
                            Icon(Icons.Outlined.Stop, contentDescription = null)
                            Spacer(Modifier.width(MaterialTheme.spacing.sm))
                            Text("停止任务")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(
                visible = running || log.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (running) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.lg,
                            vertical = MaterialTheme.spacing.md,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "任务正在后台运行",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                        Text(
                            text = "返回主页不会中断任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (log.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.lg,
                            vertical = MaterialTheme.spacing.md,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                        Text(
                            text = "任务已结束，可在通知栏查看结果",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LogView(log = log)
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExit,
            title = { Text("返回首页？") },
            text = {
                Text("任务正在后台运行，返回首页不会中断任务。之后可在首页点击「查看任务」继续看日志。")
            },
            dismissButton = {
                TextButton(onClick = onDismissExit) { Text("留在此页") }
            },
            confirmButton = {
                TextButton(onClick = onConfirmExit) { Text("返回首页") }
            },
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TaskScreenLightPreview() = PreviewTheme(false) {
    TaskScreen(
        log = "RUN: 执行任务\n✅ 签到成功",
        running = true,
        showExitDialog = false,
        onBackRequest = {},
        onDismissExit = {},
        onConfirmExit = {},
        onStop = {},
    )
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TaskScreenDarkPreview() = PreviewTheme(true) {
    TaskScreen(
        log = "RUN: 执行任务",
        running = false,
        showExitDialog = true,
        onBackRequest = {},
        onDismissExit = {},
        onConfirmExit = {},
        onStop = {},
    )
}
