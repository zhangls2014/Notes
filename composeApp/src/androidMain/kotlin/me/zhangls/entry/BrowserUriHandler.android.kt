package me.zhangls.entry

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri

@Composable
internal actual fun rememberBrowserUriHandler(): UriHandler {
  val context = LocalContext.current
  val fallback = LocalUriHandler.current
  return remember(context, fallback) { AndroidBrowserUriHandler(context, fallback) }
}

internal class AndroidBrowserUriHandler(context: Context, fallback: UriHandler) :
  UriHandler by BrowserUriHandler(
    fallback = fallback,
    openWebUri = { uri ->
      val customTab = CustomTabsIntent.Builder().setShowTitle(true).build()
      if (context !is Activity) customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      try {
        customTab.launchUrl(context, uri.toUri())
      } catch (exception: ActivityNotFoundException) {
        throw IllegalArgumentException("No browser available to open URI", exception)
      }
    },
  )
