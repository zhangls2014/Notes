package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.emailNavEntry
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.mainNavEntries
import me.zhangls.navigation.DeviceCornerNavDisplay
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1000dp-mdpi")
class HomeEmailPaneMotionTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Test fun wideDetailsAnimateWithinPersistentListScene() = assertPaneMotion(Posture())

  @Test fun foldableListUsesFullWidthUntilDetailOpens() = assertPaneMotion(
    Posture(hingeList = listOf(HingeInfo(Rect(500f, 0f, 500f, 1000f), true, true, false, false))),
  )

  private fun assertPaneMotion(posture: Posture) {
    val state = MotionState()
    setUpMotion(state, posture)
    with(state) {
      compose.runOnIdle {
        assertTrue(hostWidth > 0)
        assertEquals(hostWidth, listWidth)
        assertEquals(1, listMounts)
      }
      compose.mainClock.autoAdvance = false
      try {
        compose.runOnIdle { selected.value = license }
        val openingPositions = mutableListOf<Float>()
        val opening = sampleListWidths {
          openingPositions.add(detailX)
          listWidth
        }
        compose.runOnIdle {
          assertTrue("List must resize over multiple frames", opening.distinct().size > 2)
          assertTrue("Detail must slide into view", openingPositions.distinct().size > 2)
          assertTrue("Open detail must leave room for both panes", listWidth in 1 until hostWidth)
          assertEquals(1, listMounts)
        }
        val expandedListWidth = listWidth
        compose.runOnIdle { selected.value = project }
        val replacing = sampleListWidths { listWidth }
        compose.runOnIdle {
          assertTrue("Replacing detail must not replay pane expansion", replacing.all { it == expandedListWidth })
          assertEquals(1, listMounts)
        }
        compose.runOnIdle { closing.value = true }
        repeat(2) { compose.mainClock.advanceTimeByFrame() }
        compose.runOnIdle { assertTrue("Detail must remain composed during its exit", detailMounts > 0) }
        val closingPositions = mutableListOf<Float>()
        val closingWidths = sampleListWidths(PANE_CLOSE_DURATION_MILLIS / 16 + 3) {
          closingPositions.add(detailX)
          listWidth
        }
        compose.runOnIdle {
          assertTrue("List must expand over multiple frames: $closingWidths", closingWidths.distinct().size > 2)
          assertEquals(hostWidth, listWidth)
          assertTrue("Detail must slide out of view", closingPositions.distinct().size > 2)
          selected.value = null
          closing.value = false
        }
        sampleListWidths { listWidth }
        compose.runOnIdle {
          assertEquals(hostWidth, listWidth)
          assertEquals(0, detailMounts)
          assertEquals(1, listMounts)
        }
      } finally {
        compose.mainClock.autoAdvance = true
      }
    }
  }

  private fun setUpMotion(state: MotionState, posture: Posture) {
    android.provider.Settings.Global.putFloat(
      RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    with(state) {
      compose.setContent {
        ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1000, 1000), posture) {
          ComposeAppTheme {
            Box(Modifier.fillMaxSize().onSizeChanged { hostWidth = it.width }) {
              val entries = buildList {
                add(
                  NavEntry(list, metadata = homeMetadata(list)) {
                    DisposableEffect(Unit) {
                      listMounts++
                      onDispose {}
                    }
                    Box(Modifier.fillMaxSize().onSizeChanged { listWidth = it.width })
                  },
                )
                selected.value?.let { detail ->
                  add(
                    NavEntry(detail, metadata = homeMetadata(detail)) {
                      DisposableEffect(detail) {
                        detailMounts++
                        onDispose { detailMounts-- }
                      }
                      Box(Modifier.fillMaxSize().onGloballyPositioned { detailX = it.positionInRoot().x })
                    },
                  )
                }
              }
              DeviceCornerNavDisplay(
                entries = entries.toImmutableList(),
                sceneStrategies = rememberAppSceneStrategies<NavKey>(
                  detailClosing = closing.value,
                ).toImmutableList(),
                onBack = { closing.value = true },
              )
            }
          }
        }
      }
    }
  }

  private fun homeMetadata(key: NavKey): Map<String, Any> = entryProvider<NavKey> {
    mainNavEntries(
      homeMetadata = homeEmailListMetadata,
      openedDetail = null,
      navigateToEmailDetail = { _, _ -> },
      navigateToProfile = {},
      navigateToAbout = {},
      onLogout = {},
    )
    emailNavEntry(paneMetadata = ::homeEmailPaneMetadata) {}
  }(key).metadata

  private class MotionState {
    val list: NavKey = HomeDestination
    val license: NavKey = EmailDetailDestination(1L)
    val project: NavKey = EmailDetailDestination(2L)
    val selected = mutableStateOf<NavKey?>(null)
    val closing = mutableStateOf(false)
    var hostWidth = 0
    var listWidth = 0
    var listMounts = 0
    var detailMounts = 0
    var detailX = 0f
  }

  private fun sampleListWidths(frames: Int = 125, readWidth: () -> Int): List<Int> = buildList {
    // Two seconds of frame time permits both spring and duration-based motion to settle.
    repeat(frames) {
      compose.mainClock.advanceTimeByFrame()
      compose.runOnIdle { add(readWidth()) }
    }
  }
}
