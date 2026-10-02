package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutEntry
import me.zhangls.theme.ComposeAppTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutUiTest(private val scenario: Scenario) {
  data class Scenario(val name: String, val width: Int, val height: Int, val fontScale: Float, val dark: Boolean) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun scenarios() = listOf(
      arrayOf(Scenario("compact", 320, 400, 1f, false)),
      arrayOf(Scenario("phone", 400, 800, 1f, false)),
      arrayOf(Scenario("large-font-dark", 320, 600, 2f, true)),
      arrayOf(Scenario("tablet", 900, 1000, 1f, false)),
    )
  }

  @get:Rule val compose = createComposeRule()
  @Before fun start() { initKoin(null) }
  @After fun stop() { stopKoin() }

  private fun launch(info: AboutAppInfo) {
    compose.setContent {
      Box(Modifier.requiredSize(scenario.width.dp, scenario.height.dp).testTag("about-page")) {
        ComposeAppTheme(darkTheme = scenario.dark, fontScale = scenario.fontScale) {
          koinInject<AboutEntry>().Screen(info, onBackPressed = {})
        }
      }
    }
  }

  @Test fun installedVersionAndDottedBuildCanBeExpandedAndCollapsed() {
    launch(AboutAppInfo("2.3.1", "42.7.1"))
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}.png")
    compose.onNodeWithText("2.3.1").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Build number").assertDoesNotExist()
    compose.onNodeWithText("Version").performClick()
    compose.onNodeWithText("42.7.1").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.onNodeWithText("Build number").assertDoesNotExist()
    compose.onNodeWithText("Check for updates").performScrollTo().assertIsDisplayed()
    assertTrue(compose.onAllNodes(hasText("Check for updates") and hasClickAction()).fetchSemanticsNodes().isEmpty())
    compose.onNodeWithText("Coming soon").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    val versionRowRight = compose.onNodeWithText("Version").getUnclippedBoundsInRoot().right.value
    val badgeTextRight = compose.onNodeWithText("Coming soon").getUnclippedBoundsInRoot().right.value
    org.junit.Assert.assertEquals("Update status aligns to the row's trailing inset",
      versionRowRight - 16f, badgeTextRight, 1f)
    val titleBounds = compose.onNodeWithText("Check for updates").getUnclippedBoundsInRoot()
    val detailBounds = compose.onNodeWithText("New versions and release notes").getUnclippedBoundsInRoot()
    val badgeBounds = compose.onNodeWithText("Coming soon").getUnclippedBoundsInRoot()
    org.junit.Assert.assertEquals("Update status is centered beside the title and description",
      (titleBounds.top.value + detailBounds.bottom.value) / 2f,
      (badgeBounds.top.value + badgeBounds.bottom.value) / 2f, 1f)
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}-updates.png")
    assertTrue("Update text and status do not overlap", titleBounds.right <= badgeBounds.left)
  }

  @Test fun missingVersionIsShownAsUnknown() {
    launch(AboutAppInfo(null, null))
    compose.onNodeWithText("Unknown").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("1.0").assertDoesNotExist()
  }

  @Test
  @Config(qualifiers = "zh-rCN-w1000dp-h1200dp-mdpi")
  fun chineseBrandAndNavigationLabelsAreLocalized() {
    launch(AboutAppInfo("2.3.1", "42.7.1"))
    compose.onNodeWithText("摘星").assertIsDisplayed()
    compose.onNodeWithText("关于应用").assertIsDisplayed()
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}-zh.png")
  }
}
