package com.guyuexuan.bjxd

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guyuexuan.bjxd.ui.ConfigScreen
import com.guyuexuan.bjxd.ui.ConfigValues
import com.guyuexuan.bjxd.ui.theme.AppTheme
import com.guyuexuan.bjxd.util.StorageUtil

class ConfigActivity : ComponentActivity() {
    private lateinit var storageUtil: StorageUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storageUtil = StorageUtil.getInstance(this)
        val focusQinglong = intent.getBooleanExtra(MainActivity.EXTRA_FOCUS_QINGLONG, false)

        val initial = ConfigValues(
            manualAnswer = storageUtil.isManualAnswer,
            aiApiKey = storageUtil.aiApiKey,
            aiRequestUrl = storageUtil.aiRequestUrl,
            aiModel = storageUtil.aiModel,
            aiRequestParams = storageUtil.aiRequestParams,
            searchEnabled = storageUtil.isSearchEnabled,
            searchApiKey = storageUtil.searchApiKey,
            searchResultCount = storageUtil.searchResultCount.toString(),
            searchTimeout = storageUtil.searchTimeout.toString(),
            qinglongUrl = storageUtil.qinglongRequestUrl,
            qinglongClientId = storageUtil.qinglongClientId,
            qinglongClientSecret = storageUtil.qinglongClientSecret,
        )

        setContent {
            AppTheme {
                ConfigScreen(
                    initial = initial,
                    defaultAiUrl = StorageUtil.DEFAULT_AI_REQUEST_URL,
                    defaultAiModel = StorageUtil.DEFAULT_AI_MODEL,
                    defaultAiParams = StorageUtil.DEFAULT_AI_REQUEST_PARAMS,
                    focusQinglong = focusQinglong,
                    onBack = { finish() },
                    onOpenGlmKey = { openUrl("https://bigmodel.cn/apikey/platform") },
                    onOpenBaiduKey = { openUrl("https://console.bce.baidu.com/ai-search/qianfan/ais/console/apiKey") },
                    onSave = { values -> save(values) },
                )
            }
        }
    }

    private fun save(values: ConfigValues) {
        // AI Key 会作为 Bearer 头发给该地址，必须 HTTPS；青龙内网 HTTP 仅提示风险，不阻断
        if (values.aiRequestUrl.isNotBlank() && !values.aiRequestUrl.startsWith("https://")) {
            Toast.makeText(this, "AI 请求地址必须使用 HTTPS，否则密钥会明文传输", Toast.LENGTH_LONG)
                .show()
            return
        }
        if (values.qinglongUrl.isNotBlank() && values.qinglongUrl.startsWith("http://")) {
            Toast.makeText(
                this,
                "青龙使用 HTTP，密钥可能被同网段监听，建议改用 HTTPS",
                Toast.LENGTH_LONG
            ).show()
        }
        storageUtil.isManualAnswer = values.manualAnswer
        storageUtil.saveAiApiKey(values.aiApiKey)
        storageUtil.saveAiRequestUrl(values.aiRequestUrl)
        storageUtil.saveAiModel(values.aiModel)
        storageUtil.saveAiRequestParams(values.aiRequestParams)
        storageUtil.isSearchEnabled = values.searchEnabled
        storageUtil.saveSearchApiKey(values.searchApiKey)
        storageUtil.saveSearchResultCount(
            (values.searchResultCount.toIntOrNull() ?: 5).coerceIn(
                1,
                10
            )
        )
        storageUtil.saveSearchTimeout((values.searchTimeout.toIntOrNull() ?: 30).coerceIn(1, 600))
        storageUtil.saveQinglongRequestUrl(values.qinglongUrl)
        storageUtil.saveQinglongClientId(values.qinglongClientId)
        storageUtil.saveQinglongClientSecret(values.qinglongClientSecret)
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openUrl(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(Intent.createChooser(intent, "选择浏览器打开"))
        } catch (e: Exception) {
            Toast.makeText(this, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
}
