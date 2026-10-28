package me.zhangls.about.api

import androidx.compose.runtime.Composable

interface AboutEntry {
  @Composable
  fun Screen(appInfo: AboutAppInfo, onBack: () -> Unit, onOpenSource: () -> Unit)

  @Composable
  fun NoticesScreen(
    page: OpenSourcePage,
    anchor: String?,
    appInfo: AboutAppInfo,
    onBack: () -> Unit,
    onNavigate: (OpenSourcePage, String?) -> Unit,
  )
}
