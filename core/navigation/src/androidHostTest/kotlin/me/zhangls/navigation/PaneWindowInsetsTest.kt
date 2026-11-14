package me.zhangls.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
class PaneWindowInsetsTest {
  @get:Rule val compose = createComposeRule()

  @Test fun paneOnlyAppliesTopInsetWhenItIntersectsTheStatusBar() {
    val paneOffset = mutableStateOf(56.dp)
    val topInset = WindowInsets(top = 56.dp)
    compose.setContent {
      Box(Modifier.fillMaxSize().padding(top = paneOffset.value)) {
        PaneWindowInsets {
          Box(Modifier.fillMaxSize().windowInsetsPadding(topInset)) {
            Box(Modifier.fillMaxSize().testTag("content"))
          }
        }
      }
    }

    assertEquals(56f, compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.top, 0.5f)
    compose.runOnIdle { paneOffset.value = 0.dp }
    assertEquals(56f, compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.top, 0.5f)
  }
}
