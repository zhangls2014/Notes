package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.entry.adaptive.AdaptiveSettings
import me.zhangls.entry.adaptive.AdaptiveUsers
import me.zhangls.settings.SettingsEntryImpl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Third-party isolation controls and the real settings page restoration regression. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreferenceDialogRestorationTest {
  @get:Rule val compose = createComposeRule()

  @Test fun settingsLanguageDialogRestoresImmediatelyWhenItsRowIsBelowViewport() {
    initKoin {
      modules(module {
        single<SettingsRepository> { AdaptiveSettings() }
        single<UserRepository> { AdaptiveUsers() }
      })
    }
    try {
      val restoration = StateRestorationTester(compose)
      var height = 900
      restoration.setContent {
        MaterialTheme {
          Box(Modifier.requiredSize(400.dp, height.dp)) {
            SettingsEntryImpl().Screen(onResult = {})
          }
        }
      }
      compose.onNodeWithText("Language").performClick()
      compose.onNodeWithText("English").assertIsDisplayed()
      compose.runOnIdle { height = 160 }
      restoration.emulateSavedInstanceStateRestore()
      compose.onNodeWithText("English").assertIsDisplayed()
    } finally {
      stopKoin()
    }
  }

  @Test fun lazyItemOutsideRestoredViewportDelaysDialogUntilScrolledIntoComposition() {
    verifyRestoration(lazy = true, shrink = true)
  }

  @Test fun ordinaryColumnRestoresDialogEvenWhenItsRowIsBelowViewport() {
    verifyRestoration(lazy = false, shrink = true)
  }

  @Test fun lazyItemInsideRestoredViewportRestoresDialogImmediately() {
    verifyRestoration(lazy = true, shrink = false)
  }

  private fun verifyRestoration(lazy: Boolean, shrink: Boolean) {
    val restoration = StateRestorationTester(compose)
    val flow = MutableStateFlow<Preferences>(MapPreferences())
    // Intentionally not Compose state: the changed height is read on restoration only.
    var height = 900
    restoration.setContent {
      MaterialTheme {
        ProvidePreferenceLocals(flow = flow) {
          val modifier = Modifier.requiredSize(400.dp, height.dp).testTag("preferences")
          if (lazy) {
            LazyColumn(modifier) {
              items(10, key = { "padding-$it" }) { Text("Row $it", Modifier.height(64.dp)) }
              item(key = "language") {
                ListPreference(value = "English choice", onValueChange = {},
                  values = listOf("English choice", "Chinese choice"), title = { Text("Language") })
              }
            }
          } else {
            Column(modifier.verticalScroll(rememberScrollState())) {
              repeat(10) { Text("Row $it", Modifier.height(64.dp)) }
              ListPreference(value = "English choice", onValueChange = {},
                values = listOf("English choice", "Chinese choice"), title = { Text("Language") })
            }
          }
        }
      }
    }
    compose.onNodeWithText("Language").performClick()
    compose.onNodeWithText("Chinese choice").assertIsDisplayed()
    compose.runOnIdle { if (shrink) height = 160 }
    restoration.emulateSavedInstanceStateRestore()
    if (lazy && shrink) {
      compose.onNodeWithText("Chinese choice").assertDoesNotExist()
      compose.onNodeWithTag("preferences").performScrollToIndex(10)
    }
    compose.onNodeWithText("Chinese choice").assertIsDisplayed()
  }
}
