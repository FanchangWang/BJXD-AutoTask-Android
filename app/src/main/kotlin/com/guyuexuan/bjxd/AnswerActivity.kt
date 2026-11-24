package com.guyuexuan.bjxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guyuexuan.bjxd.service.TaskService
import com.guyuexuan.bjxd.ui.AnswerScreen
import com.guyuexuan.bjxd.ui.theme.AppTheme

class AnswerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val question = intent.getStringExtra(EXTRA_QUESTION).orEmpty()
        val optionsText = intent.getStringExtra(EXTRA_OPTIONS_TEXT).orEmpty()
        val letters = intent.getStringArrayExtra(EXTRA_LETTERS)?.toList().orEmpty()

        setContent {
            AppTheme {
                AnswerScreen(
                    question = question + optionsText,
                    options = letters,
                    onConfirm = { answer ->
                        TaskService.submitAnswer(answer)
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_QUESTION = "question"
        const val EXTRA_OPTIONS_TEXT = "options_text"
        const val EXTRA_LETTERS = "letters"
    }
}
