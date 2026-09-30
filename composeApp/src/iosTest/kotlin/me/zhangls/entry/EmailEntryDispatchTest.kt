package me.zhangls.entry

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import me.zhangls.email.EmailEntryImpl
import me.zhangls.email.api.EmailEntry
import me.zhangls.login.LoginEntryImpl
import me.zhangls.login.api.LoginEntry
import me.zhangls.settings.SettingsEntryImpl
import me.zhangls.settings.api.SettingsEntry

/** Exercise the Compose interface ABI across modules on Kotlin/Native, without UI or Koin. */
class EmailEntryDispatchTest {
  @Test
  fun realEntriesDispatchIntoFeatureImplementations() {
    val entry: EmailEntry = EmailEntryImpl()
    val recomposer = Recomposer(EmptyCoroutineContext)
    val composition = Composition(NoOpApplier(), recomposer)
    try {
      // A headless composition has no platform host. Reaching that boundary proves that
      // Native dispatched into the real feature instead of an IrLinkageError stub.
      val screens: List<@Composable () -> Unit> = listOf(
        { entry.HomeScreen(openedEmailId = null, navigateToDetail = {}) },
        { entry.FavoritesScreen(openedEmailId = 42L, navigateToDetail = {}) },
        { entry.DetailScreen(emailId = 42L, onBackPressed = {}) },
      )
      for (screen in screens) {
        val error = assertFailsWith<IllegalStateException> {
          composition.setContent { screen() }
        }
        assertTrue(error.message.orEmpty().contains("LocalHostDefaultProvider"), error.message)
      }
    } finally {
      composition.dispose()
      recomposer.cancel()
    }
  }

  @Test
  fun loginAndSettingsEntriesDispatchIntoFeatureImplementations() {
    val login: LoginEntry = LoginEntryImpl()
    val settings: SettingsEntry = SettingsEntryImpl()
    val recomposer = Recomposer(EmptyCoroutineContext)
    val composition = Composition(NoOpApplier(), recomposer)
    try {
      val screens: List<@Composable () -> Unit> = listOf(
        { login.Screen(onLoginResult = {}) },
        { settings.Screen(onResult = {}) },
      )
      for (screen in screens) {
        val error = assertFailsWith<IllegalStateException> {
          composition.setContent { screen() }
        }
        assertTrue(error.message.orEmpty().contains("LocalHostDefaultProvider"), error.message)
      }
    } finally {
      composition.dispose()
      recomposer.cancel()
    }
  }
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
  override fun insertBottomUp(index: Int, instance: Unit) = Unit
  override fun insertTopDown(index: Int, instance: Unit) = Unit
  override fun move(from: Int, to: Int, count: Int) = Unit
  override fun remove(index: Int, count: Int) = Unit
  override fun onClear() = Unit
}
