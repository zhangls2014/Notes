package me.zhangls.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.UserModel
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileState
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h320dp-mdpi")
class ProfileContentTest {
  @get:Rule val compose = createComposeRule()
  private var state by mutableStateOf(ProfileState(user = UserModel("local", "Ada Lovelace")))
  private var selections = 0
  private var back = 0

  private fun launch() {
    compose.setContent {
      ComposeAppTheme(fontScale = 2f) {
        ProfileContent(state, onBackPressed = { back++ }, onChangeAvatar = { selections++ })
      }
    }
  }

  @Test fun largeTextCanScrollToUsernameAndExplicitChangeAction() {
    launch()
    compose.onNodeWithContentDescription("Avatar").assertIsDisplayed()
    compose.onNodeWithText("Ada Lovelace").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Change avatar").performScrollTo()
      .assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
    assertEquals(1, selections)
    compose.onNodeWithContentDescription("Back").performClick()
    assertEquals(1, back)
  }

  @Test fun savingDisablesChangeAndStatusIsAnnouncedWithRetryAvailable() {
    state = state.copy(saveStatus = AvatarSaveStatus.Saving)
    launch()
    compose.onNodeWithText("Change avatar").performScrollTo().assertIsNotEnabled().performClick()
    assertEquals(0, selections)
    val saving = compose.onNodeWithText("Saving avatar…").performScrollTo()
    assertEquals(LiveRegionMode.Polite, saving.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
    compose.runOnIdle { state = state.copy(saveStatus = AvatarSaveStatus.Failed) }
    val failure = compose.onNodeWithText("Unable to save avatar. Please try again.").performScrollTo()
    assertEquals(LiveRegionMode.Polite, failure.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
    compose.onNodeWithText("Change avatar").performScrollTo().assertIsEnabled().performClick()
    assertEquals(1, selections)
  }
}
