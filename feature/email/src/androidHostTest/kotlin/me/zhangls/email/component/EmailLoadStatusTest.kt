package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import me.zhangls.email.mvi.EmailDetailState
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class EmailLoadStatusTest {
  @get:Rule val compose = createComposeRule()
  private val failed = LoadState.Error(IllegalStateException("Unavailable"))
  private val idle = LoadState.NotLoading(true)
  private val email = EmailModel(
    id = 1,
    sender = AccountModel(1, "Ada", "", "ada@example.invalid", "", "avatar_1"),
    subject = "Existing mail",
    body = "Already loaded",
    createdAt = "09:30",
  )

  @Test
  fun initialFailureIsCenteredInContentViewport() {
    launchList(emptyList(), LoadStates(failed, idle, idle))
    compose.onNodeWithText("Mail could not be loaded").assertIsDisplayed()
    val retry = compose.onNodeWithText("Retry").fetchSemanticsNode().boundsInRoot
    val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
    assertEquals(viewport.center.x, retry.center.x, 1f)
    assertTrue(retry.top > viewport.height / 3)
    compose.onNodeWithText("No emails").assertDoesNotExist()
  }

  @Test
  fun appendFailureRemainsBelowExistingMail() {
    launchList(listOf(email), LoadStates(idle, idle, failed))
    compose.onNodeWithText("Existing mail").assertIsDisplayed()
    val mail = compose.onNodeWithText("Existing mail").fetchSemanticsNode().boundsInRoot
    val error = compose.onNodeWithText("More mail could not be loaded.").fetchSemanticsNode().boundsInRoot
    assertTrue(error.top > mail.bottom)
  }

  @Test
  fun refreshFailureKeepsExistingMailAndAppearsAboveIt() {
    launchList(listOf(email), LoadStates(failed, idle, idle))
    compose.onNodeWithText("Existing mail").assertIsDisplayed()
    val mail = compose.onNodeWithText("Existing mail").fetchSemanticsNode().boundsInRoot
    val error = compose.onNodeWithText("Refresh failed. Your loaded mail is still available.")
      .fetchSemanticsNode().boundsInRoot
    assertTrue(error.bottom < mail.top)
  }

  @Test
  fun detailShowsRetryOnlyAfterFailureAndRetainsBackAction() {
    val state = androidx.compose.runtime.mutableStateOf<EmailDetailState>(EmailDetailState.Loading)
    val threads = MutableStateFlow(PagingData.empty<EmailModel>())
    var retries = 0
    var backs = 0
    compose.setContent {
      ComposeAppTheme {
        EmailDetail(
          detailState = state.value,
          threads = threads.collectAsLazyPagingItems(),
          onBack = { backs++ },
          onFavoriteClick = {},
          onRetry = { retries++ },
        )
      }
    }
    compose.onNodeWithText("Retry").assertDoesNotExist()
    compose.runOnIdle { state.value = EmailDetailState.Failed }
    compose.onNodeWithText("Retry").assertIsDisplayed().performClick()
    assertEquals(1, retries)
    compose.runOnIdle { state.value = EmailDetailState.NotFound }
    compose.onNodeWithText("Mail not found").assertIsDisplayed()
    compose.onNodeWithText("Retry").assertDoesNotExist()
    compose.onNodeWithText("Mail").assertIsDisplayed()
    compose.onNodeWithContentDescription("Back").performClick()
    assertEquals(1, backs)
  }

  private fun launchList(data: List<EmailModel>, states: LoadStates) {
    val emails = MutableStateFlow(PagingData.from(data, sourceLoadStates = states))
    compose.setContent {
      ComposeAppTheme {
        Box(Modifier.fillMaxSize().testTag("viewport")) {
          EmailPagedList(
            emailItems = emails.collectAsLazyPagingItems(),
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(),
            isFavorite = false,
            selectedItems = persistentSetOf(),
            openedEmailId = null,
            navigateToDetail = {},
            onIntent = {},
          )
        }
      }
    }
    compose.waitForIdle()
  }
}
