package me.zhangls.about

import androidx.compose.runtime.Composable
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.api.OnlineNoticeDetailDestination

class AboutEntryImpl(private val site: NotesWebsite) : AboutEntry {
  @Composable
  override fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit) {
    me.zhangls.about.ReleaseNotesDialog(version, onDismiss)
  }

  @Composable
  override fun Screen(onBack: () -> Unit, onOpenReleaseHistory: () -> Unit, onOpenNotices: () -> Unit) {
    AboutScreen(onBack, onOpenNotices, onOpenReleaseHistory)
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
  override fun ReleaseHistoryScreen(onBack: () -> Unit) {
    me.zhangls.about.ReleaseHistoryScreen(onBack, site)
  }
}
