package me.zhangls.about.api

import androidx.compose.runtime.Composable

interface AboutEntry {
  fun onForegroundChanged(isForeground: Boolean)

  @Composable
  fun UpdateHandler(showDialog: Boolean, releaseNotesVersion: String?, onReleaseNotesDismiss: () -> Unit)

  suspend fun refreshWebAssets()

  @Composable
  fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit)

  @Composable
  fun Screen(
    onBack: () -> Unit,
    onOpenReleaseHistory: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenCacheManagement: () -> Unit,
  )

  @Composable
  fun OfficialWebsiteScreen(onBack: () -> Unit)

  @Composable
  fun OnlineNoticesScreen(
    detail: OnlineNoticeDetailDestination?,
    onBack: () -> Unit,
    onOpenDetail: (OnlineNoticeDetailDestination) -> Unit,
  )

  @Composable
  fun CacheManagementScreen(onBack: () -> Unit)

  @Composable
  fun ReleaseHistoryScreen(onBack: () -> Unit)
}
