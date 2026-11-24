package com.guyuexuan.bjxd.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.ui.PreviewTheme
import com.guyuexuan.bjxd.ui.theme.spacing

@Composable
fun LogView(log: String) {
    val scrollState = rememberScrollState()

    val density = androidx.compose.ui.platform.LocalDensity.current
    val followThresholdPx = with(density) { MaterialTheme.spacing.xl.toPx() }
    LaunchedEffect(log) {
        // 仅当用户原本就在底部附近时跟随新日志；上滚查看历史时不要抢走视角
        val thresholdPx = followThresholdPx
        val atBottom = scrollState.maxValue - scrollState.value <= thresholdPx
        if (atBottom) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    if (log.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaterialTheme.spacing.xxxxxl),
            )
            Text(
                text = "还没有日志。开始任务后会在这里显示执行过程。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.spacing.lg),
    ) {
        Text(
            text = log,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@Preview(name = "Light", showBackground = true)
@Composable
private fun LogViewLightPreview() = PreviewTheme(false) {
    androidx.compose.material3.Surface { LogView(log = "RUN: 执行任务\n✅ 签到成功") }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LogViewDarkPreview() = PreviewTheme(true) {
    androidx.compose.material3.Surface { LogView(log = "") }
}
