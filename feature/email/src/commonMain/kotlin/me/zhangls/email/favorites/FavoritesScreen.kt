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
  val viewModel: EmailViewModel = koinViewModel()

  EmailList(
    viewModel = viewModel,
    isFavorite = true,
    openedEmailId = null,
    navigateToDetail = navigateToDetail,
  )
}
