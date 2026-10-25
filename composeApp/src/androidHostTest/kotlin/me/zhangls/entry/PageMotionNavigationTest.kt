package me.zhangls.entry

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageMotionNavigationTest {
  @get:Rule val compose = createComposeRule()

  @Test fun pageUsesOwnSizeWhileNavigationRemainsFixed() {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    fun advanceMotionBy(millis: Long) = compose.mainClock.advanceTimeBy(millis * PredictiveMotionDuration / 220L)
    fun advanceMotionFrame() = advanceMotionBy(16)
    val stack = mutableStateListOf(0, 1)
    lateinit var view: View
    lateinit var pageTransition: Transition<EnterExitState>
    compose.setContent {
      view = LocalView.current
        Box(Modifier.requiredSize(400.dp, 800.dp).testTag("window")) {
          Box(Modifier.fillMaxSize().padding(start = 80.dp, bottom = 64.dp)) {
            Box(Modifier.fillMaxSize()) {
              DeviceCornerNavDisplay(
                entries = stack.map { key -> NavEntry(key) {
                  if (key == 1) pageTransition = LocalNavAnimatedContentScope.current.transition
                  Box(Modifier.fillMaxSize().testTag("page-$key").background(if (key == 1) Color.Red else Color.Blue))
                } },
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                onBack = { stack.removeLast() },
                corners = DeviceCorners(40f, 40f, 40f, 40f),
              )
            }
          }
          Box(Modifier.requiredSize(80.dp, 800.dp).background(Color.Green))
          Box(Modifier.align(Alignment.BottomEnd).requiredSize(320.dp, 64.dp).background(Color.Yellow))
        }
    }
    repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
    compose.onNodeWithTag("window").captureToImage() // Flush the initial native draw before pausing the clock.
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    val backdropTint = Color.Gray.red * 0.24f
    compose.mainClock.autoAdvance = false
    fun assertCommittedFade(releaseProgress: Float = 1f) {
      var fadingFrames = 0
      var opaqueShrinkingFrames = 0
      var previousRed = 1f
      var previousBackdrop = backdropTint
      var backdropFaded = false
      val remainingFrames = kotlin.math.ceil((220f * (1f - releaseProgress) + 120f) / 16f).toInt() + 5
      repeat(remainingFrames) {
        advanceMotionFrame()
        compose.waitForIdle()
        compose.runOnIdle { view.invalidate() }
        compose.waitForIdle()
        val frame = compose.onNodeWithTag("window").captureToImage().toPixelMap()
        val red = frame[240, 400].red
        val bounds = compose.onAllNodesWithTag("page-1").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        assertTrue("Committed page fades continuously", red <= previousRed + 0.01f)
        if (bounds != null && bounds.width > 272.05f) {
          assertTrue("Remaining shrinking stays opaque after release at $releaseProgress", red > 0.99f)
          opaqueShrinkingFrames++
        }
        if (red in 0.2f..0.9f) {
          fadingFrames++
          assertTrue("Fading starts only at the completed gesture scale: release=$releaseProgress, bounds=$bounds",
            bounds != null && kotlin.math.abs(bounds.width - 272f) < 0.05f)
          val tint = frame[81, 400].red
          assertTrue("Backdrop fades only during the commit tail", tint <= previousBackdrop + 0.01f)
          if (tint < backdropTint - 0.02f) backdropFaded = true
          previousBackdrop = tint
          assertTrue("Completion keeps the navigation rail fixed", frame[10, 400].green > 0.99f)
          assertTrue("Completion keeps bottom navigation fixed", frame[200, 780] == Color.Yellow)
        }
        previousRed = red
      }
      assertTrue("Return has several fade-out frames", fadingFrames >= 3)
      assertTrue("Completion reduces backdrop opacity", backdropFaded)
      if (releaseProgress <= 0.5f) {
        assertTrue("Partial and ordinary return include opaque shrinking frames", opaqueShrinkingFrames >= 3)
      }
    }
    try {
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { advanceMotionFrame(); compose.waitForIdle() }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(40f, 400f, 0.1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { advanceMotionFrame(); compose.waitForIdle() }
      compose.runOnIdle { view.invalidate() }
      compose.waitForIdle()
      val early = compose.onNodeWithTag("window").captureToImage().toPixelMap()[81, 400]
      assertTrue("A short drag draws a lighter backdrop: $early", early.red > 0.01f && early.red < backdropTint - 0.02f)
      assertEquals("Backdrop keeps its gray color", early.red, early.green, 0.005f)
      assertTrue("A short drag leaves the previous page brighter", early.blue > 0.95f)
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { advanceMotionBy(500); compose.waitForIdle() }
      val image = compose.onNodeWithTag("window").captureToImage().toPixelMap()
      assertEquals("A full drag reaches maximum backdrop opacity", backdropTint, image[81, 400].red, 0.005f)
      assertTrue("Page offset is not compensated", image[100, 400].blue in 0.85f..0.95f)
      assertTrue("Backdrop has a gray tint", image[200, 710].red in 0.05f..0.2f &&
        image[200, 710].green in 0.05f..0.2f)
      assertTrue("Page remains opaque", image[110, 400].red > 0.99f)
      assertTrue("Scale uses page width rather than window width", image[380, 400].blue in 0.85f..0.95f)
      assertTrue("Bottom corner reveals the previous page", image[107, 677].blue in 0.85f..0.95f)
      assertTrue("Below the scaled page no background covers the previous page", image[200, 710].blue in 0.85f..0.95f)
      assertTrue("Page bottom center remains opaque", image[200, 670].red > 0.99f)
      assertTrue("Rail remains fixed", image[10, 400].green > 0.99f)
      assertTrue("Bottom navigation remains fixed", image[200, 780] == Color.Yellow)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      var sampledRestoringCorners = false
      repeat(20) {
        advanceMotionFrame()
        compose.waitForIdle()
        val restoring = compose.onNodeWithTag("window").captureToImage().toPixelMap()
        // Derive geometry from this native frame, rather than mixing it with newer semantics.
        val left = (80 until 400).first { restoring[it, 400].red > 0.99f }
        val right = (80 until 400).last { restoring[it, 400].red > 0.99f }
        val fraction = (320f - (right - left + 1)) / 48f
        if (fraction in 0.2f..0.65f) {
          assertTrue("Cancellation fades the backdrop", restoring[81, 400].red < backdropTint - 0.02f)
          val radius = 40f * fraction
          val bottom = (0 until 736).last { restoring[240, it].red > 0.99f } - 1
          assertTrue("Bottom left radius shrinks with scale: fraction=$fraction",
            restoring[ceil(left + radius + 2f).toInt(), bottom].red > 0.99f)
          assertTrue("Bottom right radius shrinks with scale: fraction=$fraction",
            restoring[floor(right - radius - 2f).toInt(), bottom].red > 0.99f)
          assertTrue("Restoration keeps a rounded contour before full scale",
            restoring[left + 1, bottom].blue > 0.85f)
          sampledRestoringCorners = true
        }
      }
      assertTrue("Cancellation includes intermediate corner restoration frames", sampledRestoringCorners)
      repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
      assertTrue("Cancellation restores the page",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[100, 400].red > 0.99f)
      // Even a short drag must restore corners on the same timeline as the page scale.
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { advanceMotionFrame(); compose.waitForIdle() }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(8f, 400f, 0.02f, BackEventCompat.EDGE_LEFT))
      }
      advanceMotionBy(100)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      var restoredScale = false
      for (frame in 1..20) {
        advanceMotionFrame()
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("page-1").fetchSemanticsNode().boundsInRoot
        val restoring = compose.onNodeWithTag("window").captureToImage().toPixelMap()
        if (bounds.width >= 319.99f) {
          assertTrue("Bottom left corner is square on the first frame at full scale",
            restoring[82, 734].red > 0.99f)
          assertTrue("Bottom right corner is square on the first frame at full scale",
            restoring[397, 734].red > 0.99f)
          restoredScale = true
          break
        }
      }
      assertTrue("Cancellation restores the page scale", restoredScale)
      advanceMotionBy(300)
      compose.waitForIdle()
      assertTrue("Cancellation restores the original square content corner",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[82, 734].red > 0.99f)
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
      }
      advanceMotionFrame()
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { advanceMotionBy(500); compose.waitForIdle() }
      compose.runOnIdle { dispatcher.onBackPressed() }
      assertCommittedFade()
      repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
      assertTrue("Completion displays the previous page",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[200, 400].blue > 0.99f)
      assertTrue("Completion reveals previous page", stack.toList() == listOf(0))
      for (progress in listOf(0.1f, 0.5f, 0.9f, 0f, null)) {
        compose.runOnIdle { stack.add(1) }
        repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
        if (progress != null) {
          compose.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
          }
          repeat(3) { advanceMotionFrame(); compose.waitForIdle() }
          compose.runOnIdle {
            dispatcher.dispatchOnBackProgressed(BackEventCompat(190f, 400f, progress, BackEventCompat.EDGE_LEFT))
          }
          repeat(3) { advanceMotionFrame(); compose.waitForIdle() }
          assertTrue("Dragging keeps the page opaque",
            compose.onNodeWithTag("window").captureToImage().toPixelMap()[240, 400].red > 0.99f)
          compose.runOnIdle { dispatcher.onBackPressed() }
        } else {
          compose.runOnIdle { stack.removeLast() }
        }
        for (frame in 1..10) {
          advanceMotionFrame()
          compose.waitForIdle()
          if (pageTransition.targetState == EnterExitState.PostExit) break
        }
        assertEquals("Return starts an exit transition", EnterExitState.PostExit, pageTransition.targetState)
        assertEquals("Ordinary and gesture return use the same commit timeline: progress=$progress, " +
          "current=${pageTransition.currentState}, target=${pageTransition.targetState}", (PredictiveMotionDuration + PageExitFadeDuration) * 1_000_000L,
          pageTransition.totalDurationNanos)
        assertCommittedFade(releaseProgress = progress ?: 0f)
        repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
        assertTrue("Both predictive and ordinary return reveal the previous page",
          compose.onNodeWithTag("window").captureToImage().toPixelMap()[240, 400].blue > 0.99f)
      }
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
