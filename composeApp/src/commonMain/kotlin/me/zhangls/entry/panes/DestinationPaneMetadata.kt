package me.zhangls.entry.panes

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.Modifier
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.EmailDetailPlaceholder

private const val NoticesSceneKey = "online-notices"
internal val noticesSceneKeys = setOf<Any>(NoticesSceneKey)
internal val noticesListMetadata = listPaneMetadata(NoticesSceneKey) {}
internal val noticesDetailMetadata = detailPaneMetadata(NoticesSceneKey)

internal val destinationPaneInsets = mapOf<Set<Any>, WindowInsetsSides>(
  noticesSceneKeys to (WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
  EmailListScene.entries.toSet() to (WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal),
)

internal val homeEmailListMetadata = listPaneMetadata(EmailListScene.Home) {
  EmailDetailPlaceholder(modifier = Modifier)
}

internal val favoritesEmailListMetadata = listPaneMetadata(EmailListScene.Favorites) {
  EmailDetailPlaceholder(modifier = Modifier)
}

internal fun emailPaneMetadata(destination: EmailDetailDestination): Map<String, Any> =
  detailPaneMetadata(destination.scene)
