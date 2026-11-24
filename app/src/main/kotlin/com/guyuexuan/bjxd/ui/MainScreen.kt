package com.guyuexuan.bjxd.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.guyuexuan.bjxd.model.User
import com.guyuexuan.bjxd.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appTitle: String,
    users: List<User>,
    taskRunning: Boolean,
    showSyncConfirm: Boolean,
    snackbarHostState: SnackbarHostState,
    onAddByLogin: () -> Unit,
    onAddByToken: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSync: () -> Unit,
    onSyncConfirmDismiss: () -> Unit,
    onSyncConfirm: () -> Unit,
    onStartTask: () -> Unit,
    onCopyToken: (String) -> Unit,
    onDeleteUser: (Int) -> Unit,
    onMoveUser: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appTitle) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = fabExpanded,
                onClick = onStartTask,
                icon = {
                    Icon(
                        imageVector = if (taskRunning) Icons.AutoMirrored.Outlined.Article else Icons.Outlined.PlayArrow,
                        contentDescription = null,
                    )
                },
                text = { Text(if (taskRunning) "查看任务" else "开始任务") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(MaterialTheme.spacing.lg),
        ) {
            item {
                Text(
                    text = "添加账号",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    EntryCard(
                        title = "手机号登录",
                        description = "在网页登录后自动获取账号",
                        icon = Icons.Outlined.Smartphone,
                        onClick = onAddByLogin,
                        modifier = Modifier.weight(1f),
                    )
                    EntryCard(
                        title = "粘贴 Token",
                        description = "已有 Token 时直接添加",
                        icon = Icons.Outlined.Key,
                        onClick = onAddByToken,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (users.isEmpty()) "还没有账号" else "已添加 ${users.size} 个账号",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = onOpenSync) {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                        Text("同步到青龙")
                    }
                }
            }
            itemsIndexed(
                items = users,
                key = { _, user: User -> user.phone },
                contentType = { _, _ -> "account" },
            ) { index, user ->
                AccountCard(
                    modifier = Modifier.animateItem(),
                    user = user,
                    order = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < users.lastIndex,
                    onCopyToken = { onCopyToken(user.token) },
                    onDelete = { onDeleteUser(index) },
                    onMoveUp = { onMoveUser(index, index - 1) },
                    onMoveDown = { onMoveUser(index, index + 1) },
                )
            }
            if (users.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(MaterialTheme.spacing.xl))
                        Text(
                            text = "选择上方任一方式添加账号后，即可执行签到、浏览和答题任务。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showSyncConfirm) {
        AlertDialog(
            onDismissRequest = onSyncConfirmDismiss,
            title = { Text("同步账号到青龙") },
            text = {
                Text("将验证双方账号：移除青龙失效账号、用较新的 Token 覆盖较旧的、补齐缺失账号，并按 BJXD 编号重新排列。青龙上的账号配置会被重写。")
            },
            dismissButton = {
                TextButton(onClick = onSyncConfirmDismiss) { Text("取消") }
            },
            confirmButton = {
                TextButton(onClick = onSyncConfirm) { Text("开始同步") }
            },
        )
    }
}

@Composable
private fun EntryCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MaterialTheme.spacing.xxl),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(
    modifier: Modifier = Modifier,
    user: User,
    order: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCopyToken: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = { menuOpen = true },
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        ListItem(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = user.nickname.ifBlank { "未命名账号" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    val expireAt = user.tokenExpireAt
                    val expireText = if (expireAt > 0L) {
                        java.text.SimpleDateFormat(
                            "MM-dd HH:mm",
                            androidx.compose.ui.platform.LocalLocale.current.platformLocale,
                        ).format(java.util.Date(expireAt))
                    } else {
                        "未知"
                    }
                    Text(
                        text = if (user.isTokenExpired) "已过期 $expireText" else "有效期 $expireText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.isTokenExpired) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            },
            supportingContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = user.maskedPhone,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "添加于 ${user.addedTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Text(
                    text = order.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "账号操作",
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("上移") },
                        onClick = { onMoveUp(); menuOpen = false },
                        enabled = canMoveUp,
                        leadingIcon = {
                            Icon(Icons.Outlined.ArrowUpward, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("下移") },
                        onClick = { onMoveDown(); menuOpen = false },
                        enabled = canMoveDown,
                        leadingIcon = {
                            Icon(Icons.Outlined.ArrowDownward, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("复制 Token") },
                        onClick = { onCopyToken(); menuOpen = false },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除账号") },
                        onClick = { onDelete(); menuOpen = false },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        },
                    )
                }
            },
        )
    }
}


@Preview(name = "Light", showBackground = true)
@Composable
private fun MainScreenLightPreview() = PreviewTheme(false) {
    MainScreen(
        appTitle = "北京现代每日任务 v2.0",
        taskRunning = false,
        showSyncConfirm = false,
        users = listOf(com.guyuexuan.bjxd.model.User("token", "李女士", "13800000000", "hid")),
        snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        onAddByLogin = {}, onAddByToken = {}, onOpenSettings = {}, onOpenSync = {},
        onSyncConfirmDismiss = {}, onSyncConfirm = {},
        onStartTask = {}, onCopyToken = {}, onDeleteUser = {}, onMoveUser = { _, _ -> },
    )
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MainScreenDarkPreview() = PreviewTheme(true) {
    MainScreen(
        appTitle = "北京现代每日任务 v2.0",
        taskRunning = true,
        showSyncConfirm = true,
        users = emptyList(),
        snackbarHostState = androidx.compose.material3.SnackbarHostState(),
        onAddByLogin = {}, onAddByToken = {}, onOpenSettings = {}, onOpenSync = {},
        onSyncConfirmDismiss = {}, onSyncConfirm = {},
        onStartTask = {}, onCopyToken = {}, onDeleteUser = {}, onMoveUser = { _, _ -> },
    )
}
