package me.zhangls.email.detail

import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel


/**
 * 邮件详情页：作为独立的导航目的地渲染。
 *
 * 外壳（导航套件）由 `AppShell` 统一提供，本页只渲染内容 —— 页面因此不需要知道
 * "自己是否被外壳包住"：导航套件占用的系统内边距已由外壳消费，页面直接用
 * `Scaffold` 给出的 padding。
 */
@Composable
internal fun EmailDetailScreen(
  emailId: Long,
  viewModel: EmailViewModel = koinViewModel(),
  onBackPressed: () -> Unit = {}
) {
  EmailDetail(
    emailId = emailId,
    viewModel = viewModel,
    onBackPressed = onBackPressed,
  )
}
