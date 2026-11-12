package me.zhangls.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.zhangls.entry.adaptive.AdaptiveSettings
import me.zhangls.entry.adaptive.AdaptiveUsers
import me.zhangls.framework.toast.ToastGlobalNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ReleaseNotesStartupTest {
  @Test fun waitsForSettingsAndKeepsAcknowledgementAcrossRootRecreation() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val settings = AdaptiveSettings()
      val users = AdaptiveUsers()
      val viewModel = AppViewModel(SavedStateHandle(), ToastGlobalNotifier(), users, settings)
      store.put("app", viewModel)
      assertFalse(viewModel.state.value.settingsLoaded)
      runCurrent()
      assertTrue(viewModel.state.value.settingsLoaded)
      assertEquals(false, viewModel.state.value.isLogin)
      assertNull(viewModel.state.value.lastReadReleaseVersion)
      viewModel.sendIntent(AppIntent.DismissReleaseNotes("1.1.0"))
      runCurrent()
      assertEquals("1.1.0", viewModel.state.value.lastReadReleaseVersion)
      store.clear()
      val recreated = AppViewModel(SavedStateHandle(), ToastGlobalNotifier(), users, settings)
      store.put("recreated", recreated)
      assertFalse(recreated.state.value.settingsLoaded)
      runCurrent()
      assertEquals("1.1.0", recreated.state.value.lastReadReleaseVersion)
      recreated.sendIntent(AppIntent.DismissReleaseNotes("1.2.0"))
      runCurrent()
      assertEquals("1.2.0", recreated.state.value.lastReadReleaseVersion)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }
}
