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
import platform.Foundation.NSNotificationCenter


private val LocalIosComposeRebuildToken = staticCompositionLocalOf { false }

fun MainViewController() = ComposeUIViewController {
  AppEntry()
}

fun MainViewController(deepLinkUrl: String?) = ComposeUIViewController {
  AppEntry(deepLinkUrl = deepLinkUrl)
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
