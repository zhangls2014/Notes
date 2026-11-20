package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
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
    compose.onNodeWithText("All emails shown").assertDoesNotExist()
  }

  @Test
  fun refreshFailureKeepsExistingMailAndAppearsAboveIt() {
    launchList(listOf(email), LoadStates(failed, idle, idle))
    compose.onNodeWithText("Existing mail").assertIsDisplayed()
    val mail = compose.onNodeWithText("Existing mail").fetchSemanticsNode().boundsInRoot
    val error = compose.onNodeWithText("Refresh failed. Your loaded mail is still available.")
      .fetchSemanticsNode().boundsInRoot
    assertTrue(error.bottom < mail.top)
    compose.onNodeWithText("All emails shown").assertDoesNotExist()
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
    compose.onNodeWithText("Mail").assertDoesNotExist()
    compose.runOnIdle { state.value = EmailDetailState.Loaded(email) }
    compose.onAllNodes(androidx.compose.ui.test.hasText("Existing mail")).fetchSemanticsNodes().let {
      assertEquals(2, it.size)
    }
    compose.runOnIdle { state.value = EmailDetailState.Loading }
    compose.onNodeWithText("Existing mail").assertDoesNotExist()
    compose.runOnIdle { state.value = EmailDetailState.Failed }
    compose.onNodeWithText("Retry").assertIsDisplayed().performClick()
    assertEquals(1, retries)
    compose.runOnIdle { state.value = EmailDetailState.NotFound }
    compose.onNodeWithText("Mail not found").assertIsDisplayed()
    compose.onNodeWithText("Retry").assertDoesNotExist()
    compose.onNodeWithText("Mail").assertDoesNotExist()
    compose.onNodeWithContentDescription("Back").performClick()
    assertEquals(1, backs)
  }

  @Test
  fun composeActionsShareOneRowWhenSpaceAllowsAndKeepTheirModes() {
    val modes = mutableListOf<String>()
    val width = androidx.compose.runtime.mutableStateOf(400.dp)
    compose.setContent {
      ComposeAppTheme {
        Box(Modifier.width(width.value)) {
          EmailDetailItem(email, onCompose = { _, mode -> modes += mode })
        }
      }
    }
    val reply = compose.onNodeWithText("Reply").fetchSemanticsNode().boundsInRoot
    val forward = compose.onNodeWithText("Forward").fetchSemanticsNode().boundsInRoot
    assertEquals(reply.center.y, forward.center.y, 1f)
    compose.onNodeWithText("Reply").performClick()
    compose.onNodeWithText("Reply all").performClick()
    compose.onNodeWithText("Forward").performClick()
    assertEquals(listOf("reply", "replyAll", "forward"), modes)
    compose.runOnIdle { width.value = 220.dp }
    val wrappedForward = compose.onNodeWithText("Forward").fetchSemanticsNode().boundsInRoot
    val wrappedReply = compose.onNodeWithText("Reply").fetchSemanticsNode().boundsInRoot
    assertTrue(wrappedForward.top > wrappedReply.bottom)
    compose.onNodeWithText("Reply all").assertIsDisplayed()
    compose.onNodeWithText("Forward").assertIsDisplayed()
  }

  @Test
  fun completedNonEmptyListShowsCenteredEndMessageAfterMail() {
    launchList(listOf(email), LoadStates(idle, idle, idle))
    compose.onNodeWithText("All emails shown").assertIsDisplayed()
    val footer = compose.onNodeWithText("All emails shown").fetchSemanticsNode().boundsInRoot
    val mail = compose.onNodeWithText("Existing mail").fetchSemanticsNode().boundsInRoot
    val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
    assertTrue(footer.top > mail.bottom)
    assertEquals(viewport.center.x, footer.center.x, 1f)
  }

  @Test
  fun emptyListDoesNotShowEndMessage() {
    launchList(emptyList(), LoadStates(idle, idle, idle))
    compose.onNodeWithText("No emails!").assertIsDisplayed()
    compose.onNodeWithText("All emails shown").assertDoesNotExist()
  }

  @Test
  fun morePagesAvailableDoesNotShowEndMessage() {
    launchList(listOf(email), LoadStates(idle, idle, LoadState.NotLoading(false)))
    compose.onNodeWithText("All emails shown").assertDoesNotExist()
  }

  @Test
  fun refreshingDoesNotShowStaleEndMessage() {
    launchList(listOf(email), LoadStates(LoadState.Loading, idle, idle))
    compose.onNodeWithText("All emails shown").assertDoesNotExist()
  }

  @Test
  fun endMessageUsesOnlyRegularBottomSpacingWithComposeButton() {
    val data = (1L..20L).map { email.copy(id = it, subject = "Mail $it") }
    val emails = MutableStateFlow(PagingData.from(data, sourceLoadStates = LoadStates(idle, idle, idle)))
    compose.setContent {
      ComposeAppTheme {
        val listState = rememberLazyListState()
        val height = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.dp) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(Modifier.fillMaxSize()) {
          EmailPagedList(
            emailItems = emails.collectAsLazyPagingItems(),
            listState = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            isFavorite = false,
            selectedItems = persistentSetOf(),
            openedEmailId = null,
            navigateToDetail = {},
            onIntent = {},
            bottomActionHeight = height.value,
          )
          Box(
            Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(end = 16.dp, bottom = 40.dp)
              .onSizeChanged { height.value = with(density) { it.height.toDp() } }
              .testTag("compose-action"),
          ) {
            NewEmailFab(isSending = false, listState = listState, onOpenDraft = {})
          }
        }
      }
    }
    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
    compose.onNodeWithText("All emails shown").assertIsDisplayed()
    val footer = compose.onNodeWithText("All emails shown").fetchSemanticsNode().boundsInRoot
    val lastMail = compose.onNodeWithText("Mail 20").fetchSemanticsNode().boundsInRoot
    val action = compose.onNodeWithTag("compose-action").fetchSemanticsNode().boundsInRoot
    assertTrue(lastMail.bottom < footer.top)
    assertEquals(8f, action.bottom + 16f - footer.bottom, 1f)
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
