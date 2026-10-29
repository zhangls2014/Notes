package me.zhangls.entry

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.api.HomeDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class HomeDraftNavigationTest {
  @get:Rule val compose = createComposeRule()

  @Test fun foldingWithOpenDraftReturnsToHome() {
    val singlePane = mutableStateOf(false)
    val visible = mutableStateOf(true)
    val detail = EmailDetailDestination(42L, EmailListScene.Home)
    val stack = NavBackStack<NavKey>(HomeDestination, detail)
    compose.setContent { KeepHomeDraftVisible(stack, singlePane.value, visible.value) }
    compose.runOnIdle { assertEquals(detail, stack.last()) }
    compose.runOnIdle { singlePane.value = true }
    compose.runOnIdle {
      assertEquals(listOf(HomeDestination), stack.toList())
      assertEquals(true, visible.value)
    }
  }

  @Test fun hostRecreationRetainsDraftVisibilityBeforeFolding() {
    val restoration = StateRestorationTester(compose)
    val singlePane = mutableStateOf(false)
    val detail = EmailDetailDestination(42L, EmailListScene.Home)
    val stack = NavBackStack<NavKey>(HomeDestination, detail)
    lateinit var visibility: MutableState<Boolean>
    restoration.setContent {
      visibility = rememberHomeDraftVisibility()
      KeepHomeDraftVisible(stack, singlePane.value, visibility.value)
    }
    compose.runOnIdle { visibility.value = true }
    restoration.emulateSavedInstanceStateRestore()
    compose.runOnIdle { singlePane.value = true }
    compose.runOnIdle {
      assertEquals(true, visibility.value)
      assertEquals(listOf(HomeDestination), stack.toList())
    }
  }

  @Test fun foldingWithoutDraftKeepsDetail() {
    val singlePane = mutableStateOf(false)
    val detail = EmailDetailDestination(42L, EmailListScene.Home)
    val stack = NavBackStack<NavKey>(HomeDestination, detail)
    compose.setContent { KeepHomeDraftVisible(stack, singlePane.value, false) }
    compose.runOnIdle { singlePane.value = true }
    compose.runOnIdle { assertEquals(detail, stack.last()) }
  }

  @Test fun draftDoesNotPopOtherScenes() {
    val detail = EmailDetailDestination(42L, EmailListScene.Favorites)
    val stack = NavBackStack<NavKey>(HomeDestination, detail)
    compose.setContent { KeepHomeDraftVisible(stack, true, true) }
    compose.runOnIdle { assertEquals(detail, stack.last()) }
  }
}
