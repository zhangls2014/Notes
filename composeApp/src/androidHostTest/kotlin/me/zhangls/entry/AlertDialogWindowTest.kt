package me.zhangls.entry

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import me.zhangls.about.AboutEntryImpl
import me.zhangls.about.api.NotesWebsite
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.FontSizePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.ui.PreferenceRow
import me.zhangls.preference.ui.PreferenceUiModel
import me.zhangls.preference.ui.ProvidePreferenceLocals
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.component.SimpleDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import me.zhangls.entry.createLegacyDiagnosedComposeRule as createComposeRule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
class AlertDialogWindowTest {
  @get:Rule val compose = createComposeRule()

  @Test fun platformDefaultStillConstrainsDialogToSystemBars() {
    compose.setContent { SimpleDialog(title = "Title", content = "Content", confirmText = "OK", onDismissRequest = {}) }
    compose.runOnIdle {
      val window = checkNotNull(checkNotNull(ShadowDialog.getLatestDialog()).window)
      assertNotEquals("Control: the default dialog excludes system bars", 0, window.attributes.fitInsetsTypes)
    }
  }

  @Test fun backDismissesConfirmationWithoutConfirming() {
    var dismissed = 0
    var confirmed = 0
    launch {
      SimpleDialog(
        title = "Title",
        content = "Content",
        confirmText = "Confirm",
        onDismissRequest = { dismissed++ },
        confirm = { confirmed++ },
        dismissText = "Cancel",
        dismiss = { dismissed++ },
      )
    }
    compose.runOnIdle {
      (checkNotNull(ShadowDialog.getLatestDialog()) as androidx.activity.ComponentDialog)
        .onBackPressedDispatcher.onBackPressed()
      assertEquals(1, dismissed)
      assertEquals(0, confirmed)
    }
  }

  @Test fun outsideTapDismissesAnnotatedConfirmationWithoutConfirming() {
    var dismissed = 0
    var confirmed = 0
    launch {
      SimpleDialog(
        title = "Title",
        content = androidx.compose.ui.text.AnnotatedString("Content"),
        confirmText = "Confirm",
        onDismissRequest = { dismissed++ },
        destructiveConfirm = true,
        confirm = { confirmed++ },
        dismissText = "Cancel",
        dismiss = { dismissed++ },
      )
    }
    compose.runOnIdle {
      val dialog = checkNotNull(ShadowDialog.getLatestDialog())
      for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
        val event = android.view.MotionEvent.obtain(0, 0, action, -1f, -1f, 0)
        try {
          dialog.onTouchEvent(event)
        } finally {
          event.recycle()
        }
      }
      assertEquals(1, dismissed)
      assertEquals(0, confirmed)
    }
    compose.onNodeWithText("Cancel").performClick()
    compose.runOnIdle {
      assertEquals(2, dismissed)
      assertEquals(0, confirmed)
    }
    compose.onNodeWithText("Confirm").performClick()
    compose.runOnIdle {
      assertEquals(2, dismissed)
      assertEquals(1, confirmed)
    }
  }

  @Test fun releaseNotesUsePlatformWindowInsets() {
    launch { AboutEntryImpl(NotesWebsite("https://website.example.test")).ReleaseNotesDialog(version = "1.1.0", onDismiss = {}) }
    compose.waitUntil(10_000) { ShadowDialog.getLatestDialog()?.isShowing == true }
    assertPlatformWindowInsets()
  }

  @Test fun simpleDialogUsesPlatformWindowInsets() {
    launch { SimpleDialog(title = "Title", content = "Content", confirmText = "OK", onDismissRequest = {}) }
    assertPlatformWindowInsets()
  }

  @Test fun darkThemeUsesPlatformWindowInsets() {
    launchPreference(PreferenceUiModel.Select(DarkThemePreference.spec, DarkThemeConfig.FOLLOW_SYSTEM) {})
    compose.onNodeWithText("Dark mode").performClick()
    assertPlatformWindowInsets()
  }

  @Test fun fontSizeUsesPlatformWindowInsets() {
    launchPreference(PreferenceUiModel.Select(FontSizePreference.spec, FontSizeConfig.STANDARD) {})
    compose.onNodeWithText("Font size").performClick()
    assertPlatformWindowInsets()
  }

  @Test fun languageUsesPlatformWindowInsets() {
    launchPreference(PreferenceUiModel.Select(LanguagePreference.spec, AppLanguage.FOLLOW_SYSTEM) {})
    compose.onNodeWithText("Language").performClick()
    assertPlatformWindowInsets()
  }

  @Test fun portraitWidthUsesMaterialAndPlatformBounds() {
    assertDialogWidth()
  }

  @Test
  @Config(qualifiers = "en-rUS-w1200dp-h900dp-land-mdpi")
  fun landscapeWidthRespectsMaterialMaximum() {
    assertDialogWidth()
  }

  @Test
  @Config(qualifiers = "en-rUS-w240dp-h400dp-mdpi")
  fun narrowWindowRespectsPlatformBounds() {
    assertDialogWidth()
  }

  @Test
  @Config(qualifiers = "en-rUS-w800dp-h360dp-land-mdpi")
  fun longDialogFitsPlatformWindowWhenBottomBarIsHidden() {
    assertVerticalSafety(top = 48, bottom = 0)
  }

  @Test
  @Config(qualifiers = "en-rUS-w800dp-h360dp-land-mdpi")
  fun longDialogFitsPlatformWindowWithLargerBottomBar() {
    assertVerticalSafety(top = 24, bottom = 48)
  }

  @Test
  @Config(qualifiers = "en-rUS-w1200dp-h900dp-land-mdpi")
  fun shortConfirmationStaysCompactInLandscape() {
    launch {
      AlertDialog(
        modifier = Modifier.testTag("short-alert"),
        onDismissRequest = {},
        title = { Text("Log out") },
        text = { Text("Log out?") },
        confirmButton = { Text("OK") },
      )
    }
    val alert = compose.onNodeWithTag("short-alert").fetchSemanticsNode()
    assertEquals(280f, alert.boundsInRoot.width, 1f)
    assertTrue(alert.config.contains(androidx.compose.ui.semantics.SemanticsProperties.PaneTitle))
  }

  private fun assertVerticalSafety(top: Int, bottom: Int) {
    lateinit var host: View
    var windowHeight = 0
    var observedTop = 0
    var observedBottom = 0
    var confirmed = false
    compose.setContent {
      host = LocalView.current
      windowHeight = LocalWindowInfo.current.containerSize.height
      observedTop = WindowInsets.safeDrawing.getTop(LocalDensity.current)
      observedBottom = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
      ComposeAppTheme(darkTheme = false) {
        AlertDialog(
          modifier = Modifier.testTag("safe-alert"),
          onDismissRequest = {},
          title = { Text("Long dialog") },
          text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
              repeat(60) { Text("Line $it") }
            }
          },
          confirmButton = { TextButton(onClick = { confirmed = true }) { Text("Confirm") } },
        )
      }
    }
    compose.runOnIdle {
      val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottom))
        .setVisible(WindowInsetsCompat.Type.statusBars(), top > 0)
        .setVisible(WindowInsetsCompat.Type.navigationBars(), bottom > 0)
        .build()
      ViewCompat.dispatchApplyWindowInsets(host, insets)
    }
    compose.waitForIdle()
    assertEquals(top, observedTop)
    assertEquals(bottom, observedBottom)
    val bounds = compose.onNodeWithTag("safe-alert").fetchSemanticsNode().boundsInRoot
    assertTrue(
      "Long dialog must fit inside the platform window: $bounds",
      bounds.height <= windowHeight + 1,
    )
    compose.onNodeWithText("Line 59").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Confirm").assertIsDisplayed().performClick()
    compose.runOnIdle { assertTrue(confirmed) }
    assertPlatformWindowInsets()
  }

  private fun assertDialogWidth() {
    var windowWidth = 0
    compose.setContent {
      windowWidth = LocalWindowInfo.current.containerSize.width
      ComposeAppTheme(darkTheme = false) {
        AlertDialog(
          modifier = Modifier.testTag("alert-width"),
          onDismissRequest = {},
          title = { Text("Title") },
          text = { Text("Content ".repeat(100), modifier = Modifier.fillMaxWidth()) },
          confirmButton = { Text("OK") },
        )
      }
    }
    compose.waitForIdle()
    val actualWidth = compose.onNodeWithTag("alert-width").fetchSemanticsNode().boundsInRoot.width
    assertTrue("Dialog must fit the application window", actualWidth <= windowWidth + 1f)
    assertTrue("Material maximum width is 560dp", actualWidth <= 561f)
    if (windowWidth >= 1200) assertEquals(560f, actualWidth, 1f)
    compose.runOnIdle {
      val window = checkNotNull(checkNotNull(ShadowDialog.getLatestDialog()).window)
      assertNotEquals("The platform owns the dialog positioning frame", 0, window.attributes.fitInsetsTypes)
    }
  }

  private fun launchPreference(model: PreferenceUiModel) {
    launch { ProvidePreferenceLocals { PreferenceRow(model) } }
  }

  private fun launch(content: @Composable () -> Unit) {
    compose.setContent { ComposeAppTheme(darkTheme = false, content = content) }
    compose.waitForIdle()
  }

  private fun assertPlatformWindowInsets() {
    compose.runOnIdle {
      val window = checkNotNull(checkNotNull(ShadowDialog.getLatestDialog()).window)
      assertNotEquals("Platform dialog insets must remain enabled", 0, window.attributes.fitInsetsTypes)
    }
  }
}
