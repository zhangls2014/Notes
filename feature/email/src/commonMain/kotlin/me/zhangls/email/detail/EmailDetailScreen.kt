package me.zhangls.email.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.waterfall.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EmailDetailScreen(
  emailId: Long,
  viewmodel: EmailViewModel = koinViewModel(),
  onBackPressed: () -> Unit = {}
) {
  EmailDetail(
    emailId = emailId,
    isFavorite = true,
    isBottomNavigationBar = true,
    viewmodel = viewmodel,
    onBackPressed = onBackPressed
  )
}
