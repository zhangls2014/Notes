package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.zhangls.email.search.EmailSearchBar
import me.zhangls.email.waterfall.EmailIntent
import me.zhangls.email.waterfall.EmailState
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

/**
 * 首页顶栏：未选中邮件时展示搜索栏，多选时切换为多选操作栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailTopBar(
  state: EmailState,
  isBottomNavigationBar: Boolean,
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  onIntent: (EmailIntent) -> Unit,
  onResultClick: (Long) -> Unit,
) {
  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
    if (state.selectedItems.isEmpty()) {
      EmailSearchBar(
        isBottomNavigationBar = isBottomNavigationBar,
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
          onIntent(EmailIntent.MultiDelete)
        },
        ActionItem(Icons.Rounded.Cancel, Res.string.email_action_cancel) {
          onIntent(EmailIntent.ClearSelectedEmail)
        },
      )
      EmailActionBar(modifier = Modifier.statusBarsPadding(), items = items, onClick = { it.onAction() })
    }
  }
}
