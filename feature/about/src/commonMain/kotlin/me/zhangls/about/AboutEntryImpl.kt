package me.zhangls.about

import androidx.compose.runtime.Composable
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourcePage
import org.koin.core.annotation.Singleton

@Singleton(binds = [AboutEntry::class])
class AboutEntryImpl : AboutEntry {
  @Composable
  override fun Screen(appInfo: AboutAppInfo, onBack: () -> Unit, onOpenSource: () -> Unit) {
    AboutScreen(appInfo, onBack, onOpenSource)
  }

  @Composable
  override fun NoticesScreen(
    page: OpenSourcePage,
    anchor: String?,
    appInfo: AboutAppInfo,
    onBack: () -> Unit,
    onNavigate: (OpenSourcePage, String?) -> Unit,
  ) {
    OpenSourceNoticesScreen(page, anchor, appInfo, onBack, onNavigate)
  }
}
