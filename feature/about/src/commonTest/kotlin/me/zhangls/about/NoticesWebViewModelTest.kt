package me.zhangls.about

import androidx.lifecycle.ViewModelStore
import me.zhangls.about.mvi.NoticesWebViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NoticesWebViewModelTest {
  @Test
  fun keepsTheSessionUntilItsNavigationEntryIsCleared() {
    val owner = NoticesWebViewModel()
    val store = ViewModelStore()
    store.put("notices", owner)
    val session = owner.session("https://website.example.test/open-source/android.html")
    var released = 0
    session.resource = object : NoticesWebResource { override fun release() { released++ } }
    assertSame(session, owner.session("https://website.example.test/open-source/android.html"))
    assertEquals(0, released)
    store.clear()
    assertEquals(1, released)
  }

  @Test
  fun changingTheUrlAndRetryingReleaseTheOldResource() {
    val owner = NoticesWebViewModel()
    var released = 0
    val first = owner.session("https://website.example.test/?theme=light")
    first.resource = object : NoticesWebResource { override fun release() { released++ } }
    val second = owner.session("https://website.example.test/?theme=dark")
    assertEquals(1, released)
    second.resource = object : NoticesWebResource { override fun release() { released++ } }
    second.state.fail()
    second.refresh()
    assertEquals(2, released)
  }
}
