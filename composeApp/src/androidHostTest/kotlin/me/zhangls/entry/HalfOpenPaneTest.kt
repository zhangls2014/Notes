package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.entry.panes.emailPaneMetadata
import me.zhangls.entry.panes.favoritesEmailListMetadata
import me.zhangls.entry.panes.homeEmailListMetadata
import me.zhangls.entry.panes.rememberAppSceneStrategies
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.navigation.DeviceCornerNavDisplay
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1000dp-mdpi")
class HalfOpenPaneTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Test fun homeBookKeepsItsPhysicalPartitions() = assertSplit(vertical = true)

  @Test fun homeTabletopKeepsItsSplit() = assertSplit(vertical = false)

  @Test fun favoritesBookKeepsItsPhysicalPartitions() =
    assertSplit(vertical = true, emailScene = EmailListScene.Favorites)

  @Test fun favoritesTabletopKeepsItsSplit() =
    assertSplit(vertical = false, emailScene = EmailListScene.Favorites)

  private fun assertSplit(vertical: Boolean, emailScene: EmailListScene = EmailListScene.Home) {
    val selected = mutableStateOf(false)
    val closing = mutableStateOf(false)
    val flat = mutableStateOf(false)
    var listBounds = Rect.Zero
    var detailX = 0f
    val hinge = if (vertical) Rect(410f, 0f, 420f, 1000f) else Rect(0f, 495f, 1000f, 505f)
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1000, 1000),
        Posture(!vertical && !flat.value, listOf(HingeInfo(hinge, flat.value, vertical, !flat.value, false))),
      ) {
        ComposeAppTheme {
          val list: NavKey = when {
            emailScene == EmailListScene.Favorites -> FavoritesDestination
            else -> HomeDestination
          }
          val detail = detailEntry(emailScene) { detailX = it }
          val metadata = when (list) {
            FavoritesDestination -> favoritesEmailListMetadata
            else -> homeEmailListMetadata
          }
          DeviceCornerNavDisplay(
            entries = buildList {
              add(
                NavEntry(list, metadata = metadata) {
                  Box(Modifier.fillMaxSize().onGloballyPositioned { listBounds = it.boundsInRoot() })
                },
              )
              if (selected.value) add(detail)
            }.toImmutableList(),
            sceneStrategies = rememberAppSceneStrategies<NavKey>(detailClosing = closing.value).toImmutableList(),
            onBack = {},
          )
        }
      }
    }
    compose.runOnIdle {
      assertTrue("Half-open list must occupy one physical partition: $listBounds", listBounds.width > 0f)
      assertTrue("Half-open must retain a split: $listBounds", listBounds.width < 1000f || listBounds.height < 1000f)
    }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(1)
    val partition = listBounds
    compose.runOnIdle { assertEquals(if (vertical) 410f else 360f, partition.width) }
    assertDetailMoves({ selected.value = true }) { detailX }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(0)
    compose.runOnIdle {
      assertEquals(partition, listBounds)
      assertEquals(if (vertical) 420f else 360f, detailX)
    }
    compose.runOnIdle { flat.value = true }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    compose.runOnIdle { flat.value = false }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    assertDetailMoves({ closing.value = true }) { detailX }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    compose.runOnIdle {
      selected.value = false
      closing.value = false
    }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(1)
  }

  private fun detailEntry(emailScene: EmailListScene, onPosition: (Float) -> Unit): NavEntry<NavKey> {
    val email = EmailDetailDestination(1L, emailScene)
    return NavEntry(email, metadata = emailPaneMetadata(email)) {
      Box(Modifier.fillMaxSize().onGloballyPositioned { onPosition(it.positionInRoot().x) })
    }
  }

  private fun assertDetailMoves(change: () -> Unit, readPosition: () -> Float) {
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { change() }
      val positions = buildList {
        repeat(24) {
          compose.mainClock.advanceTimeByFrame()
          compose.runOnIdle { add(readPosition()) }
        }
      }
      assertTrue("Detail content must move across frames: $positions", positions.distinct().size > 2)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
