package me.zhangls.about.api

import androidx.compose.runtime.Composable

interface AboutEntry {
  @Composable
  fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit)

  @Composable
  fun Screen(onBack: () -> Unit, onOpenReleaseHistory: () -> Unit, onOpenNotices: () -> Unit)

  @Composable
  fun OnlineNoticesScreen(
    detail: OnlineNoticeDetailDestination?,
    onBack: () -> Unit,
    onOpenDetail: (OnlineNoticeDetailDestination) -> Unit,
  )

  @Composable
  fun ReleaseHistoryScreen(onBack: () -> Unit)
}
