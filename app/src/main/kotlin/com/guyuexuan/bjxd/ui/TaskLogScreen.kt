package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.ui.common.LogView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskLogScreen(
    log: String,
    onBack: () -> Unit,
    onCopy: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "复制日志")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LogView(log = log)
        }
    }
}


@Preview(name = "Light", showBackground = true)
@Composable
private fun TaskLogScreenLightPreview() = PreviewTheme(false) {
    TaskLogScreen(log = "===== 任务日志 =====\n✅ 签到成功", onBack = {}, onCopy = {})
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TaskLogScreenDarkPreview() = PreviewTheme(true) {
    TaskLogScreen(log = "===== 任务日志 =====", onBack = {}, onCopy = {})
}
