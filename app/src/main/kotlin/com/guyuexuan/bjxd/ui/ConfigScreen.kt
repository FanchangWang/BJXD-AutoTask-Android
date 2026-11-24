package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.ui.theme.spacing

data class ConfigValues(
    val manualAnswer: Boolean,
    val aiApiKey: String,
    val aiRequestUrl: String,
    val aiModel: String,
    val aiRequestParams: String,
    val searchEnabled: Boolean,
    val searchApiKey: String,
    val searchResultCount: String,
    val searchTimeout: String,
    val qinglongUrl: String,
    val qinglongClientId: String,
    val qinglongClientSecret: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    initial: ConfigValues,
    defaultAiUrl: String,
    defaultAiModel: String,
    defaultAiParams: String,
    focusQinglong: Boolean,
    onBack: () -> Unit,
    onOpenGlmKey: () -> Unit,
    onOpenBaiduKey: () -> Unit,
    onSave: (ConfigValues) -> Unit,
) {
    var manualAnswer by remember { mutableStateOf(initial.manualAnswer) }
    var aiKey by remember { mutableStateOf(initial.aiApiKey) }
    var aiUrl by remember { mutableStateOf(initial.aiRequestUrl) }
    var aiModel by remember { mutableStateOf(initial.aiModel) }
    var aiParams by remember { mutableStateOf(initial.aiRequestParams) }
    var searchEnabled by remember { mutableStateOf(initial.searchEnabled) }
    var searchKey by remember { mutableStateOf(initial.searchApiKey) }
    var searchCount by remember { mutableStateOf(initial.searchResultCount) }
    var searchTimeout by remember { mutableStateOf(initial.searchTimeout) }
    var qlUrl by remember { mutableStateOf(initial.qinglongUrl) }
    var qlId by remember { mutableStateOf(initial.qinglongClientId) }
    var qlSecret by remember { mutableStateOf(initial.qinglongClientSecret) }
    var revealAiKey by remember { mutableStateOf(false) }
    var revealSearchKey by remember { mutableStateOf(false) }
    var revealQlSecret by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(focusQinglong) {
        if (focusQinglong) {
            listState.animateScrollToItem(4)
            snackbarHostState.showSnackbar("请先填写青龙面板地址、CLIENT_ID 和 CLIENT_SECRET")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    onSave(
                        ConfigValues(
                            manualAnswer = manualAnswer,
                            aiApiKey = aiKey.trim(),
                            aiRequestUrl = aiUrl.trim(),
                            aiModel = aiModel.trim(),
                            aiRequestParams = aiParams.trim(),
                            searchEnabled = searchEnabled,
                            searchApiKey = searchKey.trim(),
                            searchResultCount = searchCount.trim(),
                            searchTimeout = searchTimeout.trim(),
                            qinglongUrl = qlUrl.trim(),
                            qinglongClientId = qlId.trim(),
                            qinglongClientSecret = qlSecret.trim(),
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
            ) {
                Text("保存配置")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.lg),
        ) {
            item {
                Text(
                    text = "答题顺序：历史答案、手动选择、AI 与联网搜索、随机。任务在后台执行，完成后通知栏可查看日志。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SectionCard(title = "答题方式") {
                    SwitchRow(
                        label = "弹窗或通知手动选择答案",
                        checked = manualAnswer,
                        onChange = { manualAnswer = it },
                    )
                    Text(
                        text = "关闭后全自动答题。开启时，退到后台会在通知栏让你选择答案。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SectionCard(title = "AI 接口（推荐智谱 GLM，免费）") {
                    SecretField(
                        label = "AI API Key",
                        value = aiKey,
                        onChange = { aiKey = it },
                        revealed = revealAiKey,
                        onToggleReveal = { revealAiKey = !revealAiKey },
                        supporting = "在智谱控制台创建",
                    )
                    LinkButton(text = "没有 Key？打开智谱控制台", onClick = onOpenGlmKey)
                    OutlinedTextField(
                        value = aiUrl,
                        onValueChange = { aiUrl = it },
                        label = { Text("请求地址") },
                        supportingText = { Text("OpenAI 兼容接口") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = aiModel,
                        onValueChange = { aiModel = it },
                        label = { Text("模型名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = aiParams,
                        onValueChange = { aiParams = it },
                        label = { Text("附加请求参数（JSON）") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            aiUrl = defaultAiUrl
                            aiModel = defaultAiModel
                            aiParams = defaultAiParams
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("恢复智谱 GLM 默认参数")
                    }
                }
            }
            item {
                SectionCard(title = "联网搜索（百度千帆，固定）") {
                    SwitchRow(
                        label = "启用百度联网搜索",
                        checked = searchEnabled,
                        onChange = { searchEnabled = it },
                    )
                    SecretField(
                        label = "百度搜索 API Key",
                        value = searchKey,
                        onChange = { searchKey = it },
                        revealed = revealSearchKey,
                        onToggleReveal = { revealSearchKey = !revealSearchKey },
                        supporting = "格式 bce-v3/...，每月免费 1500 次",
                    )
                    LinkButton(text = "没有 Key？打开百度千帆控制台", onClick = onOpenBaiduKey)
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                        OutlinedTextField(
                            value = searchCount,
                            onValueChange = { searchCount = it },
                            label = { Text("结果条数") },
                            supportingText = { Text("1-10") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = searchTimeout,
                            onValueChange = { searchTimeout = it },
                            label = { Text("超时（秒）") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SectionCard(title = "青龙面板（可选，用于账号同步）") {
                    OutlinedTextField(
                        value = qlUrl,
                        onValueChange = { qlUrl = it },
                        label = { Text("面板地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = qlId,
                        onValueChange = { qlId = it },
                        label = { Text("应用 CLIENT_ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecretField(
                        label = "应用 CLIENT_SECRET",
                        value = qlSecret,
                        onChange = { qlSecret = it },
                        revealed = revealQlSecret,
                        onToggleReveal = { revealQlSecret = !revealQlSecret },
                        supporting = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    supporting: String?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = onToggleReveal) {
                Icon(
                    imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (revealed) "隐藏 $label" else "显示 $label",
                )
            }
        },
    )
}

@Composable
private fun LinkButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("$text ↗")
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ConfigScreenLightPreview() = PreviewTheme(false) {
    ConfigScreen(
        initial = ConfigValues(
            false,
            "",
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            "glm-4.5-flash",
            "{\"do_sample\": false}",
            true,
            "",
            "5",
            "30",
            "",
            "",
            ""
        ),
        defaultAiUrl = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_REQUEST_URL,
        defaultAiModel = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_MODEL,
        defaultAiParams = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_REQUEST_PARAMS,
        focusQinglong = false,
        onBack = {}, onOpenGlmKey = {}, onOpenBaiduKey = {}, onSave = {},
    )
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ConfigScreenDarkPreview() = PreviewTheme(true) {
    ConfigScreen(
        initial = ConfigValues(
            false,
            "",
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            "glm-4.5-flash",
            "{\"do_sample\": false}",
            true,
            "",
            "5",
            "30",
            "",
            "",
            ""
        ),
        defaultAiUrl = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_REQUEST_URL,
        defaultAiModel = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_MODEL,
        defaultAiParams = com.guyuexuan.bjxd.util.StorageUtil.DEFAULT_AI_REQUEST_PARAMS,
        focusQinglong = false,
        onBack = {}, onOpenGlmKey = {}, onOpenBaiduKey = {}, onSave = {},
    )
}
