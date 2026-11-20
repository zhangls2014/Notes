package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.persistentSetOf
import me.zhangls.data.model.EmailModel
import me.zhangls.theme.ComposeAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A distinct SDK sandbox keeps Paging's cached UI dispatcher tied to this rule's frame clock.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class EmailPullRefreshTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun pullRefreshReloadsAnEmptyList() {
    val loads = java.util.concurrent.atomic.AtomicInteger()
    val pager = androidx.paging.Pager(androidx.paging.PagingConfig(pageSize = 20)) {
      object : androidx.paging.PagingSource<Int, EmailModel>() {
        override fun getRefreshKey(state: androidx.paging.PagingState<Int, EmailModel>): Int? = null
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EmailModel> {
          loads.incrementAndGet()
          return LoadResult.Page(emptyList(), null, null)
        }
      }
    }
    compose.setContent {
      ComposeAppTheme {
        Box(Modifier.fillMaxSize().testTag("viewport")) {
          EmailPagedList(
            emailItems = pager.flow.collectAsLazyPagingItems(),
            listState = rememberLazyListState(),
            contentPadding = PaddingValues(),
            isFavorite = true,
            selectedItems = persistentSetOf(),
            openedEmailId = null,
            navigateToDetail = {},
            onIntent = {},
          )
        }
      }
    }
    compose.waitForIdle()
    compose.waitUntil(5_000) {
      compose.mainClock.advanceTimeBy(100)
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      loads.get() == 1
    }
    compose.onNodeWithTag("viewport").performTouchInput { swipeDown() }
    compose.waitForIdle()
    compose.waitUntil(5_000) {
      compose.mainClock.advanceTimeBy(100)
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      loads.get() > 1
    }
    compose.waitUntil(5_000) {
      compose.mainClock.advanceTimeBy(100)
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      compose.onNodeWithText("No emails!").isDisplayed()
    }
    compose.onNodeWithText("No emails!").assertIsDisplayed()
  }
}
