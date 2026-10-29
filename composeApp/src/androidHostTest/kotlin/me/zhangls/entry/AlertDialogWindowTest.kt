package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.zhangls.about.AboutEntryImpl
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
class AlertDialogWindowTest {
  @get:Rule val compose = createComposeRule()

  @Test fun platformDefaultStillConstrainsDialogToSystemBars() {
    compose.setContent { SimpleDialog(title = "Title", content = "Content", confirmText = "OK") }
    compose.runOnIdle {
      val window = checkNotNull(checkNotNull(ShadowDialog.getLatestDialog()).window)
      assertNotEquals("Control: the default dialog excludes system bars", 0, window.attributes.fitInsetsTypes)
    }
  }

  @Test fun releaseNotesUseFullWindowForCentering() {
    launch { AboutEntryImpl().ReleaseNotesDialog(version = "1.1.0", onDismiss = {}) }
    compose.waitUntil(10_000) { ShadowDialog.getLatestDialog()?.isShowing == true }
    assertFullWindow()
  }

  @Test fun simpleDialogUsesFullWindowForCentering() {
    launch { SimpleDialog(title = "Title", content = "Content", confirmText = "OK") }
    assertFullWindow()
  }

  @Test fun darkThemeUsesFullWindowForCentering() {
    launchPreference(PreferenceUiModel.Select(DarkThemePreference.spec, DarkThemeConfig.FOLLOW_SYSTEM) {})
    compose.onNodeWithText("Dark mode").performClick()
    assertFullWindow()
  }

  @Test fun fontSizeUsesFullWindowForCentering() {
    launchPreference(PreferenceUiModel.Select(FontSizePreference.spec, FontSizeConfig.STANDARD) {})
    compose.onNodeWithText("Font size").performClick()
    assertFullWindow()
  }

  @Test fun languageUsesFullWindowForCentering() {
    launchPreference(PreferenceUiModel.Select(LanguagePreference.spec, AppLanguage.FOLLOW_SYSTEM) {})
    compose.onNodeWithText("Language").performClick()
    assertFullWindow()
  }

  private fun launchPreference(model: PreferenceUiModel) {
    launch { ProvidePreferenceLocals { PreferenceRow(model) } }
  }

  private fun launch(content: @Composable () -> Unit) {
    compose.setContent { ComposeAppTheme(darkTheme = false, content = content) }
    compose.waitForIdle()
  }

  private fun assertFullWindow() {
    compose.runOnIdle {
      val window = checkNotNull(checkNotNull(ShadowDialog.getLatestDialog()).window)
      assertEquals("System bars must not shrink the alert dialog positioning frame", 0, window.attributes.fitInsetsTypes)
    }
  }
}
