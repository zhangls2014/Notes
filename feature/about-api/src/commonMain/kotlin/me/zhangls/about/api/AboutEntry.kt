package me.zhangls.about.api

import androidx.compose.runtime.Composable

interface AboutEntry {
  @Composable
  fun Screen(appInfo: AboutAppInfo, onBackPressed: () -> Unit)
}
