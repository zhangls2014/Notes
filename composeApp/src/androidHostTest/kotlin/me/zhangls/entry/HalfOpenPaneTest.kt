package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
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
import me.zhangls.entry.panes.noticesDetailMetadata
import me.zhangls.entry.panes.noticesListMetadata
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
import me.zhangls.entry.createDiagnosedComposeRule as createComposeRule

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1000dp-mdpi")
class HalfOpenPaneTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Test fun emptyDetailHintAlignsWithListContentBelowToolbar() {
    compose.setContent {
      ComposeAppTheme {
        androidx.compose.foundation.layout.Row(Modifier.requiredSize(1000.dp, 800.dp)) {
          androidx.compose.material3.Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { me.zhangls.theme.component.CenteredTopAppBar(title = "Favorites") },
          ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
              androidx.compose.material3.Text("No emails")
            }
          }
          me.zhangls.main.EmailDetailPlaceholder(Modifier.weight(1f))
        }
      }
    }
    val listHint = compose.onNodeWithText("No emails").fetchSemanticsNode().boundsInRoot
    val detailHint = compose.onNodeWithText("Select an email to read").fetchSemanticsNode().boundsInRoot
    assertEquals(listHint.center.y, detailHint.center.y, 1f)
  }

  @Test fun homeBookKeepsItsPhysicalPartitions() = assertSplit(vertical = true)

  @Test fun homeTabletopExpandsListUntilDetailSelected() = assertSplit(vertical = false)

  @Test fun favoritesBookKeepsItsPhysicalPartitions() =
    assertSplit(vertical = true, emailScene = EmailListScene.Favorites)

  @Test fun favoritesTabletopExpandsListUntilDetailSelected() =
    assertSplit(vertical = false, emailScene = EmailListScene.Favorites)

  @Test fun noticesTabletopExpandsListUntilDetailSelected() = assertSplit(vertical = false, notices = true)

  @Test fun narrowTabletopDetailUsesFullHeight() {
    var detailBounds = Rect.Zero
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(400, 1000),
        Posture(true, listOf(HingeInfo(Rect(0f, 495f, 400f, 505f), false, false, true, false))),
      ) {
        ComposeAppTheme {
          Box(Modifier.requiredSize(400.dp, 1000.dp)) {
            DeviceCornerNavDisplay(
              entries = listOf(
                NavEntry<NavKey>(HomeDestination, metadata = noticesListMetadata) {},
                NavEntry<NavKey>(EmailDetailDestination(1L), metadata = noticesDetailMetadata) {
                  Box(Modifier.fillMaxSize().onGloballyPositioned { detailBounds = it.boundsInRoot() })
                },
              ).toImmutableList(),
              sceneStrategies = rememberAppSceneStrategies<NavKey>().toImmutableList(),
              onBack = {},
            )
          }
        }
      }
    }
    compose.runOnIdle {
      assertEquals(400f, detailBounds.width)
      assertEquals(1000f, detailBounds.height)
    }
  }

  private fun assertSplit(
    vertical: Boolean,
    emailScene: EmailListScene = EmailListScene.Home,
    notices: Boolean = false,
  ) {
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
          val detail = detailEntry(emailScene, notices) { detailX = it }
          val metadata = when (list) {
            FavoritesDestination -> favoritesEmailListMetadata
            else -> homeEmailListMetadata
          }
          DeviceCornerNavDisplay(
            entries = buildList {
              add(
                NavEntry(list, metadata = if (notices) noticesListMetadata else metadata) {
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
      assertEquals("Horizontal hinge must not split vertically", 1000f, listBounds.height)
      if (vertical) assertTrue(listBounds.width < 1000f) else assertEquals(1000f, listBounds.width)
    }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(if (vertical) 1 else 0)
    val partition = listBounds
    compose.runOnIdle { assertEquals(if (vertical) 410f else 1000f, partition.width) }
    assertDetailMoves({ selected.value = true }) { detailX }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(0)
    compose.runOnIdle {
      assertEquals(if (vertical) partition else Rect(0f, 0f, 360f, 1000f), listBounds)
      assertEquals(if (vertical) 420f else 360f, detailX)
    }
    val selectedPartition = listBounds
    compose.runOnIdle { flat.value = true }
    compose.runOnIdle { assertEquals(selectedPartition, listBounds) }
    compose.runOnIdle { flat.value = false }
    compose.runOnIdle { assertEquals(selectedPartition, listBounds) }
    assertDetailMoves({ closing.value = true }) { detailX }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    compose.runOnIdle {
      selected.value = false
      closing.value = false
    }
    compose.runOnIdle { assertEquals(partition, listBounds) }
    compose.onAllNodesWithText("Select an email to read").assertCountEquals(if (vertical) 1 else 0)
  }

  private fun detailEntry(emailScene: EmailListScene, notices: Boolean, onPosition: (Float) -> Unit): NavEntry<NavKey> {
    val email = EmailDetailDestination(1L, emailScene)
    return NavEntry(email, metadata = if (notices) noticesDetailMetadata else emailPaneMetadata(email)) {
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
