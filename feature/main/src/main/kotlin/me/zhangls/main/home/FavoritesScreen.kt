package me.zhangls.main.home

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
  EmailList(
    viewmodel = viewmodel,
    isFavorite = true,
    isBottomNavigationBar = isBottomNavigationBar,
    openedEmailId = null,
    navigateToDetail = navigateToDetail,
  )
}
