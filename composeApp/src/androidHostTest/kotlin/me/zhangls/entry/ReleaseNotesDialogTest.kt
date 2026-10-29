package me.zhangls.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    compose.onNodeWithText("Unreleased").assertIsDisplayed()
    compose.onNodeWithText("Added").assertIsDisplayed()
    compose.onNodeWithText("Improved").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Fixed").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Got it").performClick()
    compose.onNodeWithText("What’s new in 1.1.0").assertDoesNotExist()
    compose.runOnIdle { assertEquals(1, dismissed) }
  }

  @Test fun categoriesFollowAddedImprovedFixedOrder() {
    launchCurrentVersion()
    val headings = compose.onAllNodes(hasText("Added") or hasText("Improved") or hasText("Fixed"))
      .fetchSemanticsNodes().map { it.config[SemanticsProperties.Text].single().text }
    assertEquals(listOf("Added", "Improved", "Fixed"), headings)
  }

  @Test fun categoryHeadingsAreBold() {
    launchCurrentVersion()
    for (label in listOf("Added", "Improved", "Fixed")) {
      val layouts = mutableListOf<TextLayoutResult>()
      compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
      assertEquals("Category heading: $label", FontWeight.Bold, layouts.single().layoutInput.style.fontWeight)
    }
  }

  private fun launchCurrentVersion() {
    compose.setContent {
      ComposeAppTheme {
        AboutEntryImpl().ReleaseNotesDialog(version = "1.1.0", onDismiss = {})
      }
    }
    compose.waitUntil(10_000) {
      compose.onAllNodes(hasText("Added")).fetchSemanticsNodes().isNotEmpty()
    }
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

  @Test
  @Config(qualifiers = "en-rUS-w970dp-h443dp-mdpi")
  fun largeTextInShortWindowKeepsChangesScrollableAndDismissReachable() {
    var dismissed = false
    compose.setContent {
      ComposeAppTheme(darkTheme = false, fontScale = 2F) {
        AboutEntryImpl().ReleaseNotesDialog(version = "1.1.0", onDismiss = { dismissed = true })
      }
    }
    compose.waitUntil(10_000) {
      compose.onAllNodes(androidx.compose.ui.test.hasText("Got it")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("Keep dialogs centered", substring = true).performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Got it").assertIsDisplayed().performClick()
    compose.runOnIdle { org.junit.Assert.assertTrue(dismissed) }
  }
}
