package me.zhangls.entry

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertTrue
import me.zhangls.preference.ui.ProvidePreferenceLocals

/** Rendering controlled settings must not initialize a platform preference store. */
class PreferenceLocalsTest {
  @Test
  fun settingsLocalsCanBeProvidedWithoutAnApplicationPreferenceDomain() {
    val recomposer = Recomposer(EmptyCoroutineContext)
    val composition = Composition(PreferenceNoOpApplier(), recomposer)
    var rendered = false
    try {
      composition.setContent {
        ProvidePreferenceLocals { rendered = true }
      }
      assertTrue(rendered)
    } finally {
      composition.dispose()
      recomposer.cancel()
    }
  }
}

private class PreferenceNoOpApplier : AbstractApplier<Unit>(Unit) {
  override fun insertBottomUp(index: Int, instance: Unit) = Unit
  override fun insertTopDown(index: Int, instance: Unit) = Unit
  override fun move(from: Int, to: Int, count: Int) = Unit
  override fun remove(index: Int, count: Int) = Unit
  override fun onClear() = Unit
}
