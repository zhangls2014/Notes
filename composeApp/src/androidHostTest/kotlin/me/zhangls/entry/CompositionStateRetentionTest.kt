package me.zhangls.entry

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.AboutNavigation
import me.zhangls.email.api.EmailNavigation
import me.zhangls.login.api.LoginNavigation
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.MainNavigation
import me.zhangls.main.api.TabDestination
import me.zhangls.profile.api.ProfileNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class CompositionStateRetentionTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun tabContentRetainsRememberedInstanceAcrossShellBranches() {
    var selected by mutableStateOf<TabDestination?>(null)
    var observed: Any? = null
    compose.setContent {
      TabFadeThrough(selected, onSelectTab = {}) { _, _, _ ->
        val instance = remember { Any() }
        SideEffect { observed = instance }
      }
    }
    lateinit var initial: Any
    compose.runOnIdle {
      initial = checkNotNull(observed)
      selected = HomeDestination
    }
    compose.runOnIdle {
      assertSame(initial, observed)
      selected = null
    }
    compose.runOnIdle { assertSame(initial, observed) }
  }

  @Test
  fun restoredStackDoesNotAppendItsInitialEntriesAgain() {
    val restoration = StateRestorationTester(compose)
    val configuration = SavedStateConfiguration {
      serializersModule = listOf(
        MainNavigation,
        LoginNavigation,
        EmailNavigation,
        ProfileNavigation,
        AboutNavigation,
      ).fold(SerializersModule {}) { module, contribution -> module + contribution.navModule }
    }
    lateinit var navigation: AppNavigationState
    restoration.setContent {
      navigation = rememberAppNavigationState(configuration, persistentListOf<NavKey>(HomeDestination))
    }
    compose.runOnIdle { navigation.backStack.add(AboutDestination) }
    restoration.emulateSavedInstanceStateRestore()
    compose.runOnIdle {
      assertEquals(listOf(HomeDestination, AboutDestination), navigation.backStack.toList())
    }
  }
}
