package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.email.emailNavEntry
import me.zhangls.entry.adaptive.AdaptiveEmails
import me.zhangls.entry.adaptive.AdaptiveUsers
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.navigation.DeviceCornerNavDisplay
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeEmailToolbarTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Before fun start() {
    initKoin {
      modules(
        module {
          single<EmailsRepository> { AdaptiveEmails() }
          single<UserRepository> { AdaptiveUsers() }
        },
      )
    }
  }

  @After fun stop() {
    stopKoin()
  }

  @Test fun detailUsesCloseBesideListAndBackWhenWindowNarrows() = assertToolbar(EmailListScene.Home)

  @Test fun favoritesDetailUsesCloseBesideListAndBackWhenWindowNarrows() = assertToolbar(EmailListScene.Favorites)

  @OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)
  private fun assertToolbar(scene: EmailListScene) {
    val width = mutableIntStateOf(1000)
    var closed = 0
    val list: NavKey = if (scene == EmailListScene.Home) HomeDestination else FavoritesDestination
    val detail = EmailDetailDestination(1L, scene)
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width.intValue, 1200),
        Posture(),
      ) {
        ComposeAppTheme {
          val provider = entryProvider<NavKey> {
            emailNavEntry(paneMetadata = ::homeEmailPaneMetadata) { closed++ }
          }
          val listMetadata = if (scene == EmailListScene.Home) {
            homeEmailListMetadata
          } else {
            ListDetailSceneStrategy.listPane(sceneKey = scene, detailPlaceholder = {})
          }
          DeviceCornerNavDisplay(
            entries = listOf(
              // Keep the list role but omit its own toolbar to inspect only detail actions.
              NavEntry<NavKey>(list, metadata = listMetadata) {
                Box(Modifier.fillMaxSize())
              },
              provider(detail),
            ).toImmutableList(),
            sceneStrategies = rememberAppSceneStrategies<NavKey>().toImmutableList(),
            onBack = { closed++ },
          )
        }
      }
    }
    compose.onNodeWithContentDescription("Close detail").assertIsDisplayed().performClick()
    compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    compose.runOnIdle {
      assertEquals(1, closed)
      width.intValue = 400
    }
    compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
    compose.onAllNodesWithContentDescription("Close detail").assertCountEquals(0)
    compose.runOnIdle { assertEquals(2, closed) }
  }
}
