package me.zhangls.about

import androidx.compose.runtime.Composable
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourcePage

class AboutEntryImpl : AboutEntry {
  @Composable
  override fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit) {
    me.zhangls.about.ReleaseNotesDialog(version, onDismiss)
  }

  @Composable
  override fun ProjectScreen(url: String, onBack: () -> Unit) {
    ExternalProjectScreen(url, onBack)
  }

  @Composable
  override fun Screen(onBack: () -> Unit, onOpenSource: () -> Unit, onOpenReleaseHistory: () -> Unit) {
    AboutScreen(onBack, onOpenSource, onOpenReleaseHistory)
  }

  @Composable
  override fun ReleaseHistoryScreen(onBack: () -> Unit) {
    me.zhangls.about.ReleaseHistoryScreen(onBack)
  }

  @Composable
  override fun NoticesScreen(
    page: OpenSourcePage,
    anchor: String?,
    onBack: () -> Unit,
    onNavigate: (OpenSourcePage, String?) -> Unit,
    onExternalLink: (String) -> Unit,
  ) {
    OpenSourceNoticesScreen(page, anchor, onBack, onNavigate, onExternalLink)
  }
}
