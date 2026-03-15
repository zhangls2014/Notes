package me.zhangls.main.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.main.EmailViewModel
import me.zhangls.main.compose.EmailList

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun FavoritesScreen(
  isBottomNavigationBar: Boolean,
  viewmodel: EmailViewModel,
  navigateToDetail: (Long) -> Unit
) {
  val padding = if (isBottomNavigationBar) {
    WindowInsets.systemBars.exclude(WindowInsets.navigationBars).asPaddingValues()
  } else {
    WindowInsets.systemBars.asPaddingValues()
  }
  val emailListState = rememberLazyListState()

  EmailList(
    contentPadding = padding,
    emailListState = emailListState,
    selectedItems = emptySet(),
    viewmodel = viewmodel,
    isFavorite = true,
    openedEmailId = null,
    navigateToDetail = navigateToDetail,
  )
}
