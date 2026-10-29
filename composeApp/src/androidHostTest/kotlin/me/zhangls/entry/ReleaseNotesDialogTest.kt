package me.zhangls.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.zhangls.about.AboutEntryImpl
import me.zhangls.about.api.AboutEntry
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReleaseNotesDialogTest {
  @get:Rule val compose = createComposeRule()

  @Test fun currentVersionAppearsAndCanBeAcknowledged() {
    val entry: AboutEntry = AboutEntryImpl()
    var visible by mutableStateOf(true)
    var dismissed = 0
    compose.setContent {
      ComposeAppTheme(darkTheme = false) {
        if (visible) {
          entry.ReleaseNotesDialog(
            version = "1.1.0",
            onDismiss = {
              dismissed++
              visible = false
            },
          )
        }
      }
    }
    compose.waitUntil(10_000) {
      compose.onAllNodes(androidx.compose.ui.test.hasText("What’s new in 1.1.0")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("What’s new in 1.1.0").assertIsDisplayed()
    compose.onNodeWithText("Got it").performClick()
    compose.onNodeWithText("What’s new in 1.1.0").assertDoesNotExist()
    compose.runOnIdle { assertEquals(1, dismissed) }
  }

  @Test fun missingCurrentVersionDoesNotUseNewestRecord() {
    val entry: AboutEntry = AboutEntryImpl()
    compose.setContent {
      ComposeAppTheme(darkTheme = false) {
        entry.ReleaseNotesDialog(version = "9.0.0", onDismiss = { error("No dialog to dismiss") })
      }
    }
    compose.waitForIdle()
    compose.onNodeWithText("Got it").assertDoesNotExist()
  }
}
