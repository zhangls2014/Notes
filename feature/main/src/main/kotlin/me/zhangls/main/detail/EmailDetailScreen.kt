package me.zhangls.main.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.main.EmailViewModel
import me.zhangls.main.compose.EmailDetail
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