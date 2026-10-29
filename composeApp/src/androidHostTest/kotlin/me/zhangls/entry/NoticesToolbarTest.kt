package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.navigation.DeviceCornerNavDisplay
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NoticesToolbarTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Before fun start() {
    initKoin(null)
  }

  @After fun stop() {
    stopKoin()
  }

  @Test fun detailUsesCloseBesideListAndBackWhenWindowNarrows() {
    val width = mutableIntStateOf(1000)
    var closed = 0
    val list = OpenSourceNoticesDestination()
    val detail = OpenSourceNoticesDestination(OpenSourcePage.LICENSES, "license-MIT")
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width.intValue, 1200),
        Posture(),
      ) {
        ComposeAppTheme {
          DeviceCornerNavDisplay(
            entries = listOf(
              // Keep the list role but omit its own toolbar to inspect only detail actions.
              NavEntry(list, metadata = noticesPaneMetadata(list)) { Box(Modifier.fillMaxSize()) },
              NavEntry(detail, metadata = noticesPaneMetadata(detail)) {
                koinInject<AboutEntry>().NoticesScreen(
                  page = detail.page,
                  anchor = detail.anchor,

                  onBack = { closed++ },
                  onNavigate = { _, _ -> },
                  onExternalLink = {},
                )
              },
            ).toImmutableList(),
            sceneStrategies = rememberAppSceneStrategies<OpenSourceNoticesDestination>().toImmutableList(),
            onBack = { closed++ },
          )
        }
      }
    }
    compose.onNodeWithContentDescription("Close detail").assertIsDisplayed().performClick()
    compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    compose.runOnIdle {
      assertEquals(1, closed)
      width.intValue = 400
    }
    compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
    compose.onAllNodesWithContentDescription("Close detail").assertCountEquals(0)
    compose.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(0)
    compose.runOnIdle { assertEquals(2, closed) }
  }
}
