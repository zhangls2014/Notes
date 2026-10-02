package me.zhangls.email.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.MutableStateFlow
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import me.zhangls.email.mvi.EmailIntent
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class EmailListAccessibilityTest {
  @get:Rule val compose = createComposeRule()
  private var selectedItems by mutableStateOf(emptySet<Long>())
  private val opened = mutableListOf<Long>()
  private val favorites = mutableListOf<Long>()

  private fun launch(isFavorite: Boolean = false) {
    val email = EmailModel(
      id = 1,
      sender = AccountModel(1, "Ada", "Lovelace", "ada@example.test", "", "avatar_1"),
      subject = "Accessible mail",
      body = "Message content",
      createdAt = "09:30",
    )
    val emails = MutableStateFlow(PagingData.from(
      listOf(email),
      sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true)),
    ))
    compose.setContent {
      ComposeAppTheme {
        EmailPagedList(
          emailItems = emails.collectAsLazyPagingItems(),
          listState = rememberLazyListState(),
          contentPadding = PaddingValues(),
          isFavorite = isFavorite,
          selectedItems = selectedItems,
          openedEmailId = null,
          navigateToDetail = { opened += it },
          onIntent = { intent ->
            when (intent) {
              is EmailIntent.UpdateSelectedEmail -> selectedItems = if (intent.emailId in selectedItems) {
                selectedItems - intent.emailId
              } else {
                selectedItems + intent.emailId
              }
              is EmailIntent.UpdateFavorite -> favorites += intent.emailId
              else -> error("Unexpected intent: $intent")
            }
          },
        )
      }
    }
    compose.waitForIdle()
  }

  private fun assertLabels(click: String, longClick: String) {
    val semantics = compose.onNodeWithText("Accessible mail").fetchSemanticsNode().config
    assertEquals(click, semantics[SemanticsActions.OnClick].label)
    assertEquals(longClick, semantics[SemanticsActions.OnLongClick].label)
  }

  @Test fun homeActionsTrackSelectionAndKeepFavoriteIndependent() {
    launch()
    val card = compose.onNodeWithText("Accessible mail")
    assertLabels("Open email", "Select email")
    card.assertIsNotSelected().performClick()
    compose.runOnIdle { assertEquals(listOf(1L), opened) }
    card.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
    card.assertIsSelected()
    assertLabels("Deselect email", "Deselect email")
    card.performClick()
    card.assertIsNotSelected()
    assertLabels("Open email", "Select email")

    // Another selected email keeps multi-select mode active for this unselected row.
    compose.runOnIdle { selectedItems = setOf(2L) }
    assertLabels("Select email", "Select email")
    card.performClick().assertIsSelected()
    assertLabels("Deselect email", "Deselect email")
    compose.onNodeWithContentDescription("Favorite").performClick()
    compose.runOnIdle {
      assertEquals(setOf(1L, 2L), selectedItems)
      assertEquals(listOf(1L), favorites)
      assertEquals(listOf(1L), opened)
    }
    card.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
    card.assertIsNotSelected()
    assertLabels("Select email", "Select email")
  }

  @Test fun favoritesOnlyExposeAnOpenActionEvenWhenHomeHasSelection() {
    selectedItems = setOf(1L)
    launch(isFavorite = true)
    val card = compose.onNodeWithText("Accessible mail")
    val semantics = card.fetchSemanticsNode().config
    assertFalse("Favorites must not expose an ineffective long-click", SemanticsActions.OnLongClick in semantics)
    assertEquals("Open email", semantics[SemanticsActions.OnClick].label)
    card.assertIsNotSelected().performClick()
    compose.runOnIdle {
      assertEquals(listOf(1L), opened)
      assertEquals(setOf(1L), selectedItems)
    }
  }

  @Test
  @Config(qualifiers = "zh-rCN-w400dp-h800dp-mdpi")
  fun actionLabelsUseChineseResources() {
    launch()
    assertLabels("打开邮件", "选择邮件")
    compose.onNodeWithText("Accessible mail").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
    assertLabels("取消选择邮件", "取消选择邮件")
  }
}
