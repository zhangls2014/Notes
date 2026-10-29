package me.zhangls.about.api

import androidx.compose.runtime.Composable

interface AboutEntry {
  @Composable
  fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit)

  @Composable
  fun ProjectScreen(url: String, onBack: () -> Unit)

  @Composable
  fun Screen(onBack: () -> Unit, onOpenSource: () -> Unit, onOpenReleaseHistory: () -> Unit)

  @Composable
  fun ReleaseHistoryScreen(onBack: () -> Unit)

  @Composable
  fun NoticesScreen(
    page: OpenSourcePage,
    anchor: String?,
    onBack: () -> Unit,
    onNavigate: (OpenSourcePage, String?) -> Unit,
    onExternalLink: (String) -> Unit,
  )
}
