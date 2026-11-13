package me.zhangls.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.createFromText
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.UserModel
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileEdit
import me.zhangls.profile.mvi.ProfileField
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.profile.mvi.ProfileState
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        ProfileContent(state, onBack = { back++ }, onChangeAvatar = { selections++ })
      }
    }
  }

  @Test
  @Config(qualifiers = "en-rUS-w1000dp-h320dp-mdpi")
  fun draggingTheSideGutterScrollsProfile() {
    launch()
    val avatar = compose.onNodeWithContentDescription("Avatar")
    val before = avatar.fetchSemanticsNode().positionInRoot.y
    compose.onRoot().performTouchInput {
      swipe(start = Offset(40f, height - 32f), end = Offset(40f, 100f))
    }
    assertTrue(avatar.fetchSemanticsNode().positionInRoot.y < before)
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

  @Test
  @Config(qualifiers = "en-rUS-w400dp-h800dp-mdpi")
  fun emailFieldsAppearOnlyAfterThePreviousStepSucceeds() {
    var edit by mutableStateOf(ProfileEdit(ProfileField.Email, "account"))
    compose.setContent {
      ComposeAppTheme {
        ProfileIdentityEditor(edit) { intent ->
          when (intent) {
            is ProfileIntent.UpdatePassword -> edit = edit.copy(password = intent.value)
            is ProfileIntent.UpdateEmail -> edit = edit.copy(email = intent.value, sentEmail = null)
            ProfileIntent.ConfirmEdit -> {
              edit = if (!edit.passwordVerified) edit.copy(passwordVerified = true)
              else edit.copy(sentEmail = edit.email)
            }
            else -> error("Unexpected intent")
          }
        }
      }
    }
    compose.onNodeWithText("New email").assertDoesNotExist()
    compose.onNodeWithText("Email verification code").assertDoesNotExist()
    compose.onNodeWithText("Verify password").assertIsNotEnabled()
    compose.onNodeWithText("Confirm change").assertDoesNotExist()
    val password = compose.onNodeWithText("Current password")
    assertEquals(ContentType.Password, password.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentType))
    password.performSemanticsAction(SemanticsActions.OnFillData) {
      it(checkNotNull(FillableData.createFromText("example password")))
    }
    compose.runOnIdle { assertEquals("example password", edit.password) }
    compose.onNodeWithText("Verify password").performClick()
    compose.onNodeWithText("Verify password").assertDoesNotExist()
    password.assertIsNotEnabled()
    assertEquals(null, password.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentType))
    compose.onNodeWithText("Email verification code").assertDoesNotExist()
    val sendCode = compose.onNodeWithText("Send verification code").assertIsNotEnabled()
    compose.onNode(hasSetTextAction() and hasText("New email")).performTextInput("new@example.com")
    sendCode.performClick()
    sendCode.assertDoesNotExist()
    compose.onNodeWithText("Confirm change").assertIsDisplayed()
    compose.onNodeWithText("Email verification code").assertIsDisplayed()
    compose.onNodeWithText("The new email takes effect after verification.").assertDoesNotExist()
    compose.onNodeWithText("To resend the code", substring = true).assertDoesNotExist()
    compose.onNode(hasSetTextAction() and hasText("New email")).performTextInput("2")
    compose.onNodeWithText("Email verification code").assertDoesNotExist()
  }

  @Test
  @Config(qualifiers = "en-rUS-w400dp-h800dp-mdpi")
  fun closingEditorSlidesUpBeforeLeavingComposition() {
    var edit by mutableStateOf<ProfileEdit?>(ProfileEdit(ProfileField.Username, "account", "Ada"))
    compose.setContent {
      ComposeAppTheme {
        AnimatedProfileIdentityEditor(edit) { intent ->
          if (intent == ProfileIntent.CloseEdit) edit = null
        }
      }
    }
    val title = compose.onNodeWithText("Change username")
    val openedTop = title.fetchSemanticsNode().positionInRoot.y
    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithText("Cancel").performClick()
      compose.mainClock.advanceTimeBy(176)
      assertTrue(title.fetchSemanticsNode().positionInRoot.y < openedTop)
      compose.mainClock.advanceTimeBy(250)
      title.assertDoesNotExist()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun profileRowsStayInPlaceAndDisableEditingDuringSubmission() {
    val user = UserModel("account", "Ada", authOrigin = "https://example.test", email = "ada@example.test")
    state = ProfileState(user = user)
    compose.setContent {
      ComposeAppTheme {
        ProfileContent(state, onBack = {}, onChangeAvatar = {}, onEditUsername = {}, onEditEmail = {})
      }
    }
    val username = compose.onNodeWithText("Username").assertHasClickAction().assertIsEnabled()
    val initialBounds = username.fetchSemanticsNode().boundsInRoot
    compose.runOnIdle { state = state.copy(edit = ProfileEdit(ProfileField.Email, user.id, busy = true)) }
    username.assertHasClickAction().assertIsNotEnabled()
    assertEquals(initialBounds, username.fetchSemanticsNode().boundsInRoot)
    compose.onNodeWithText("Email").assertHasClickAction().assertIsNotEnabled()
    compose.runOnIdle { state = state.copy(edit = null) }
    username.assertIsEnabled()
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
