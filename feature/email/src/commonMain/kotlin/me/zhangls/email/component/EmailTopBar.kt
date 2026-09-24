package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.zhangls.email.search.EmailSearchBar
import me.zhangls.email.mvi.EmailIntent
import me.zhangls.email.mvi.EmailState
import me.zhangls.theme.component.SimpleDialog
import me.zhangls.theme.icon.Cancel
import me.zhangls.theme.icon.Delete
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_cancel
import notes.feature.email.generated.resources.email_action_cancel_favorite
import notes.feature.email.generated.resources.email_action_delete
import notes.feature.email.generated.resources.email_action_favorite
import notes.feature.email.generated.resources.email_delete_confirm_message
import notes.feature.email.generated.resources.email_delete_confirm_title
import org.jetbrains.compose.resources.stringResource

/**
 * 首页顶栏：未选中邮件时展示搜索栏，多选时切换为多选操作栏。
 *
 * 两个分支各自处理系统内边距，且**不对称是有意的**：
 * - 搜索栏走 [EmailSearchBar] → `AppBarWithSearch`，后者自带
 *   `systemBarsForVisualComponents.only(Horizontal + Top)`，已经贴好状态栏；
 * - 多选操作栏是 [EmailActionBar]（`HorizontalFloatingToolbar`），浮动工具栏没有
 *   "内置内边距"这回事，必须自己补 [statusBarsPadding]。
 *
 * 两个分支互斥，所以这里不会重复留白。加这个注释是为了避免后来者看到不对称
 * 就"顺手统一"，那会让搜索栏被顶下去一个状态栏的高度。
 * 导航套件那一侧的内边距不在这里处理 —— 已由外壳统一消费（`AppShell`）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailTopBar(
  state: EmailState,
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  onIntent: (EmailIntent) -> Unit,
  onResultClick: (Long) -> Unit,
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }
  LaunchedEffect(state.selectedItems.isEmpty()) {
    if (state.selectedItems.isEmpty()) showDeleteConfirmation = false
  }

  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
    if (state.selectedItems.isEmpty()) {
      EmailSearchBar(
        scrollBehavior = scrollBehavior,
        onResultClick = onResultClick
      )
    } else {
      // 动作在构造时即绑定到具体 Intent，不依赖字符串资源身份分发
      val items = listOf(
        ActionItem(Icons.Rounded.StarFill, Res.string.email_action_favorite) {
          onIntent(EmailIntent.MultiFavorite)
        },
        ActionItem(Icons.Rounded.Star, Res.string.email_action_cancel_favorite) {
          onIntent(EmailIntent.MultiCancelFavorite)
        },
        ActionItem(Icons.Rounded.Delete, Res.string.email_action_delete) {
          showDeleteConfirmation = true
        },
        ActionItem(Icons.Rounded.Cancel, Res.string.email_action_cancel) {
          onIntent(EmailIntent.ClearSelectedEmail)
        },
      )
      EmailActionBar(modifier = Modifier.statusBarsPadding(), items = items, onClick = { it.onAction() })
    }
  }

  if (showDeleteConfirmation && state.selectedItems.isNotEmpty()) {
    SimpleDialog(
      title = stringResource(Res.string.email_delete_confirm_title),
      content = stringResource(Res.string.email_delete_confirm_message, state.selectedItems.size),
      confirmText = stringResource(Res.string.email_action_delete),
      confirm = {
        showDeleteConfirmation = false
        onIntent(EmailIntent.MultiDelete)
      },
      dismissText = stringResource(Res.string.email_action_cancel),
      dismiss = { showDeleteConfirmation = false },
    )
  }
}
