package me.zhangls.entry.panes

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.Modifier
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.EmailDetailPlaceholder

private const val NoticesSceneKey = "open-source-notices"
internal val destinationPaneInsets = mapOf(
  setOf<Any>(NoticesSceneKey) to (WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
  EmailListScene.entries.toSet() to (WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal),
)

private val noticesListMetadata = listPaneMetadata(NoticesSceneKey) {}

internal fun noticesPaneMetadata(destination: OpenSourceNoticesDestination): Map<String, Any> =
  if (destination.page == OpenSourcePage.LIBRARIES && destination.projectUrl == null) {
    noticesListMetadata
  } else {
    detailPaneMetadata(NoticesSceneKey)
  }

internal val homeEmailListMetadata = listPaneMetadata(EmailListScene.Home) {
  EmailDetailPlaceholder(modifier = Modifier)
}

internal val favoritesEmailListMetadata = listPaneMetadata(EmailListScene.Favorites) {
  EmailDetailPlaceholder(modifier = Modifier)
}

internal fun emailPaneMetadata(destination: EmailDetailDestination): Map<String, Any> =
  detailPaneMetadata(destination.scene)
