package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.guyuexuan.bjxd.ui.theme.spacing

@Composable
fun AnswerScreen(
    question: String,
    options: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(MaterialTheme.spacing.xl)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            ) {
                Text(
                    text = "选择答案",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    options.forEach { letter ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadioButton(
                                selected = selected == letter,
                                onClick = { selected = letter },
                                modifier = Modifier.semantics {
                                    contentDescription = "选项 $letter"
                                },
                            )
                            Text(letter, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("稍后") }
                    Button(
                        onClick = { selected?.let(onConfirm) },
                        enabled = selected != null,
                    ) { Text("提交答案") }
                }
            }
        }
    }
}


@Preview(name = "Light", showBackground = true)
@Composable
private fun AnswerScreenLightPreview() = PreviewTheme(false) {
    AnswerScreen(
        question = "冬季雨雪天气，哪种驾驶模式能减少打滑？\nA. 经济模式\nB. 雪地模式",
        options = listOf("A", "B"),
        onConfirm = {},
        onDismiss = {})
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AnswerScreenDarkPreview() = PreviewTheme(true) {
    AnswerScreen(question = "题目", options = listOf("A", "B", "C"), onConfirm = {}, onDismiss = {})
}
