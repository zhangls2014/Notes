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
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.email.api.EmailDetailDestination
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

  @Test fun homeBookKeepsItsPhysicalPartitions() = assertSplit(notices = false, vertical = true)

  @Test fun homeTabletopKeepsItsSplit() = assertSplit(notices = false, vertical = false)

  @Test fun noticesBookKeepsItsPhysicalPartitions() = assertSplit(notices = true, vertical = true)

  @Test fun noticesTabletopKeepsItsSplit() = assertSplit(notices = true, vertical = false)

  private fun assertSplit(notices: Boolean, vertical: Boolean) {
    val selected = mutableStateOf(false)
    val closing = mutableStateOf(false)
    val flat = mutableStateOf(false)
    var listBounds = Rect.Zero
    var detailX = 0f
    val hinge = if (vertical) Rect(495f, 0f, 505f, 1000f) else Rect(0f, 495f, 1000f, 505f)
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1000, 1000),
        Posture(!vertical && !flat.value, listOf(HingeInfo(hinge, flat.value, vertical, !flat.value, false))),
      ) {
        ComposeAppTheme {
          val list: NavKey = if (notices) OpenSourceNoticesDestination() else HomeDestination
          val detail = detailEntry(notices) { detailX = it }
          val metadata = if (notices) noticesPaneMetadata(OpenSourceNoticesDestination()) else homeEmailListMetadata
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
    assertPlaceholder(notices)
    val partition = listBounds
    assertDetailMoves({ selected.value = true }) { detailX }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(0)
    compose.runOnIdle { assertEquals(partition, listBounds) }
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
    assertPlaceholder(notices)
  }

  private fun detailEntry(notices: Boolean, onPosition: (Float) -> Unit): NavEntry<NavKey> {
    val notice = OpenSourceNoticesDestination(OpenSourcePage.LICENSES, "license-MIT")
    val email = EmailDetailDestination(1L)
    val key: NavKey = if (notices) notice else email
    val metadata = if (notices) noticesPaneMetadata(notice) else homeEmailPaneMetadata(email)
    return NavEntry(key, metadata = metadata) {
      Box(Modifier.fillMaxSize().onGloballyPositioned { onPosition(it.positionInRoot().x) })
    }
  }

  private fun assertPlaceholder(notices: Boolean) {
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(if (notices) 0 else 1)
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
