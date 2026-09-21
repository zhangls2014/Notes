package me.zhangls.email.favorites

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailList
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FavoritesScreen(
  navigateToDetail: (Long) -> Unit
) {
  // key 与首页区分：两个 tab 处于同一个 NavEntry，不区分就会共用同一个实例
  val viewModel: EmailViewModel = koinViewModel(key = EmailViewModel.KEY_FAVORITES)

  EmailList(
    viewModel = viewModel,
    isFavorite = true,
    openedEmailId = null,
    navigateToDetail = navigateToDetail,
  )
}
