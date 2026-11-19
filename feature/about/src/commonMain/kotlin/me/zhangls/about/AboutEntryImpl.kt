package me.zhangls.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.api.OnlineNoticeDetailDestination
import me.zhangls.about.mvi.UpdateController
import org.koin.compose.koinInject

class AboutEntryImpl(
  private val site: NotesWebsite,
  private val foregroundChanged: (Boolean) -> Unit = {},
  private val updateWebAssets: suspend () -> Unit = {},
) : AboutEntry {
  override fun onForegroundChanged(isForeground: Boolean) = foregroundChanged(isForeground)

  @Composable
  override fun UpdateHandler(showDialog: Boolean, releaseNotesVersion: String?, onReleaseNotesDismiss: () -> Unit) {
    if (!showDialog) return
    val updates = koinInject<UpdateController>()
    val status by updates.updateStatus.collectAsStateWithLifecycle()
    val releases by rememberReleaseHistory()
    if (releaseNotesVersion != null && releases == null) return
    if (releases?.getOrNull()?.any { it.version == releaseNotesVersion } == true) {
      me.zhangls.about.ReleaseNotesDialog(requireNotNull(releaseNotesVersion), onReleaseNotesDismiss)
    } else {
      UpdateCheckDialog(status, updates::dismissUpdateResult, { updates.checkForUpdates() }, updates::updateAction)
    }
  }

  override suspend fun refreshWebAssets() = updateWebAssets()

  @Composable
  override fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit) {
    me.zhangls.about.ReleaseNotesDialog(version, onDismiss)
  }

  @Composable
  override fun Screen(
    onBack: () -> Unit,
    onOpenReleaseHistory: () -> Unit,
    onOpenNotices: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenCacheManagement: () -> Unit,
  ) {
    AboutScreen(onBack, onOpenNotices, onOpenReleaseHistory, onOpenWebsite, onOpenCacheManagement)
  }

  @Composable
  override fun OfficialWebsiteScreen(onBack: () -> Unit) {
    me.zhangls.about.OfficialWebsiteScreen(onBack, site)
  }

  @Composable
  override fun OnlineNoticesScreen(
    detail: OnlineNoticeDetailDestination?,
    onBack: () -> Unit,
    onOpenDetail: (OnlineNoticeDetailDestination) -> Unit,
  ) {
    OnlineNoticesPage(site, detail, onBack, onOpenDetail)
  }

  @Composable
  override fun CacheManagementScreen(onBack: () -> Unit) {
    me.zhangls.about.CacheManagementScreen(onBack)
  }

  @Composable
  override fun ReleaseHistoryScreen(onBack: () -> Unit) {
    me.zhangls.about.ReleaseHistoryScreen(onBack, site)
  }
}
