package me.zhangls.entry

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.UriHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AndroidBrowserUriHandlerTest {
  @Test fun launchesCustomTabWithoutPinningABrowserPackage() {
    val context = RuntimeEnvironment.getApplication()
    AndroidBrowserUriHandler(context, fallback()).openUri("https://example.com/path?q=1#section")
    val intent = shadowOf(context).nextStartedActivity
    assertEquals(Intent.ACTION_VIEW, intent.action)
    assertEquals("https://example.com/path?q=1#section", intent.dataString)
    assertEquals(null, intent.`package`)
    assertTrue(intent.hasExtra("android.support.customtabs.extra.SESSION"))
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  @Test fun missingBrowserIsReportedAsUriHandlerFailure() {
    val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
      override fun startActivity(intent: Intent, options: Bundle?): Unit =
        throw ActivityNotFoundException("No browser installed")
    }
    assertFailsWith<IllegalArgumentException> {
      AndroidBrowserUriHandler(context, fallback()).openUri("https://example.com")
    }
  }

  private fun fallback() = object : UriHandler {
    override fun openUri(uri: String) = error("Unexpected fallback")
  }
}
