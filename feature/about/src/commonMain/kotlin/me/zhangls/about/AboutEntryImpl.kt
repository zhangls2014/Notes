package me.zhangls.about

import androidx.compose.runtime.Composable
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutEntry
import org.koin.core.annotation.Singleton

@Singleton(binds = [AboutEntry::class])
class AboutEntryImpl : AboutEntry {
  @Composable
  override fun Screen(appInfo: AboutAppInfo, onBackPressed: () -> Unit) {
    AboutScreen(appInfo, onBackPressed)
  }
}
