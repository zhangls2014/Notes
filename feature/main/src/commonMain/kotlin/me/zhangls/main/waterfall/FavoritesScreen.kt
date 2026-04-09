package me.zhangls.main.waterfall

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.main.compose.EmailList
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun FavoritesScreen(
  isBottomNavigationBar: Boolean,
  navigateToDetail: (Long) -> Unit
) {
  val viewmodel: EmailViewModel = koinViewModel()

  EmailList(
    viewmodel = viewmodel,
    isFavorite = true,
    isBottomNavigationBar = isBottomNavigationBar,
    openedEmailId = null,
    navigateToDetail = navigateToDetail,
  )
}
