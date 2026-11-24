package com.guyuexuan.bjxd.ui

import androidx.compose.runtime.Composable
import com.guyuexuan.bjxd.ui.theme.AppTheme

@Composable
fun PreviewTheme(dark: Boolean, content: @Composable () -> Unit) {
    AppTheme(darkTheme = dark, dynamicColor = false, content = content)
}
