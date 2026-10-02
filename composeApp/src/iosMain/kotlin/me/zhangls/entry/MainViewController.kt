package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.LocalUIViewController
import me.zhangls.framework.toast.IosSystemToast
import me.zhangls.framework.toast.SystemToast
import platform.Foundation.NSNotificationCenter


private val LocalIosComposeRebuildToken = staticCompositionLocalOf { false }

fun MainViewController() = ComposeUIViewController {
  AppEntry()
}

fun MainViewController(deepLinkUrl: String?) = ComposeUIViewController {
  AppEntry(deepLinkUrl = deepLinkUrl)
}

@Composable
internal actual fun rememberSystemToast(): SystemToast {
  val host = LocalUIViewController.current
  val systemToast = remember(host) { IosSystemToast(host) }
  DisposableEffect(systemToast) {
    onDispose { systemToast.close() }
  }
  return systemToast
}

@Composable
private fun AppEntry(deepLinkUrl: String? = null) {
  var rebuildToken by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    val observer = NSNotificationCenter.defaultCenter.addObserverForName(
      name = LanguageManager.LANGUAGE_DID_CHANGE_NOTIFICATION,
      `object` = null,
      queue = null
    ) { _ ->
      rebuildToken = rebuildToken.not()
    }

    onDispose {
      NSNotificationCenter.defaultCenter.removeObserver(observer)
    }
  }

  CompositionLocalProvider(LocalIosComposeRebuildToken provides rebuildToken) {
    App(
      deepLinkUrl = deepLinkUrl,
      onLanguageChanged = { language ->
        LanguageManager.updateLanguage(language)
      }
    )
  }
}
