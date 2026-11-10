package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import me.zhangls.framework.toast.IosSystemToast
import me.zhangls.framework.toast.SystemToast
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

private val LocalIosComposeRebuildToken = staticCompositionLocalOf { false }

class IosAppHost {
  private val pendingDeepLink = mutableStateOf<String?>(null)

  val viewController: UIViewController = pendingDeepLink.let { link ->
    ComposeUIViewController {
      AppEntry(
        deepLinkUrl = link.value,
        onDeepLinkConsume = { link.value = null },
      )
    }
  }

  fun updateDeepLink(url: String?) {
    pendingDeepLink.value = url
  }
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
private fun AppEntry(deepLinkUrl: String?, onDeepLinkConsume: () -> Unit) {
  var rebuildToken by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    val observer = NSNotificationCenter.defaultCenter.addObserverForName(
      name = LanguageManager.LANGUAGE_DID_CHANGE_NOTIFICATION,
      `object` = null,
      queue = null,
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
      onDeepLinkConsume = onDeepLinkConsume,
      deviceAuthenticator = IosDeviceAuthenticator,
      onLanguageChange = { language ->
        LanguageManager.updateLanguage(language)
      },
    )
  }
}
