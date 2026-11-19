package me.zhangls.entry

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AppForegroundObserverTest {
  @Test fun onlyVisibilityChangesAreForwardedAndConfigurationStopsAreIgnored() {
    val owner = object : LifecycleOwner {
      override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }
    var configurationChange = false
    val events = mutableListOf<Boolean>()
    val observer = AppForegroundObserver({ configurationChange }, events::add)
    owner.lifecycle.addObserver(observer)
    owner.lifecycle.currentState = Lifecycle.State.RESUMED
    owner.lifecycle.currentState = Lifecycle.State.STARTED // A dialog or permission prompt only pauses.
    owner.lifecycle.currentState = Lifecycle.State.RESUMED
    assertEquals(listOf(true), events)
    configurationChange = true
    owner.lifecycle.currentState = Lifecycle.State.CREATED
    owner.lifecycle.currentState = Lifecycle.State.RESUMED
    assertEquals(listOf(true, true), events) // The shared controller coalesces repeated foreground events.
    configurationChange = false
    owner.lifecycle.currentState = Lifecycle.State.CREATED
    owner.lifecycle.currentState = Lifecycle.State.RESUMED
    assertEquals(listOf(true, true, false, true), events)
    owner.lifecycle.removeObserver(observer)
  }
}
