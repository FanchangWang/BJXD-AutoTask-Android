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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.ui.common.LogView
import com.guyuexuan.bjxd.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    log: String,
    running: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("青龙同步") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(
                visible = running || log.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (running) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "正在同步账号，完成后可返回",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.lg,
                            vertical = MaterialTheme.spacing.md,
                        ),
                    )
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
                            text = "同步已结束，可返回",
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
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SyncScreenLightPreview() = PreviewTheme(false) {
    SyncScreen(
        log = "===== 青龙账号双向同步 =====\n✓ BJXD1 有效",
        running = true,
        snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        onBack = {},
    )
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SyncScreenDarkPreview() = PreviewTheme(true) {
    SyncScreen(
        log = "同步结束。",
        running = false,
        snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        onBack = {},
    )
}
