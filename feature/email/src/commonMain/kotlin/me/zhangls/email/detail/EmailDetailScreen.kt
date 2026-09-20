package me.zhangls.email.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EmailDetailScreen(
  emailId: Long,
  viewModel: EmailViewModel = koinViewModel(),
  onBackPressed: () -> Unit = {}
) {
  // 独立导航目的地：内容占满整屏（isStandalone），不参与导航套件的内容内边距换算
  EmailDetail(
    emailId = emailId,
    isStandalone = true,
    viewModel = viewModel,
    onBackPressed = onBackPressed
  )
}
