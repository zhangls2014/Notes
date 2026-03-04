package me.zhangls.main.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.zhangls.main.EmailViewModel
import me.zhangls.main.compose.EmailDetail

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EmailDetailScreen(
  emailId: Long,
  viewmodel: EmailViewModel = hiltViewModel(),
  onBackPressed: () -> Unit = {}
) {
  EmailDetail(
    emailId = emailId,
    viewmodel = viewmodel,
    onBackPressed = onBackPressed
  )
}