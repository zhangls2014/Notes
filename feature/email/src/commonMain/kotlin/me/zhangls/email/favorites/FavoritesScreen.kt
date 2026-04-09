package me.zhangls.email.favorites

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailList
import me.zhangls.email.waterfall.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FavoritesScreen(
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
