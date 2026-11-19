package me.zhangls.entry

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.NotesWebsite
import me.zhangls.theme.ComposeAppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
class AutomaticUpdateTest {
  @get:Rule val compose = createComposeRule()
  private val requests = AtomicInteger()
  private val site = NotesWebsite("https://website.example.test")
  private var build = 2
  private var fail = false
  private var responseGate = CompletableDeferred(Unit)
  private lateinit var client: HttpClient
  private lateinit var entry: AboutEntry
  private var visible by mutableStateOf(true)
  private var aboutVisible by mutableStateOf(false)
  private var releaseNotesVersion by mutableStateOf<String?>(null)

  @Before fun start() {
    initKoin { modules(module { single<Context> { RuntimeEnvironment.getApplication() } }) }
    provideAboutAppInfo(channel = "global")
    val engine = object : HttpClientEngineBase("AutomaticUpdateTest") {
      override val config = HttpClientEngineConfig()

      @OptIn(InternalAPI::class)
      override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val manifest = data.url.encodedPath.startsWith("/updates/")
        if (manifest) requests.incrementAndGet()
        responseGate.await()
        val body = if (manifest) {
          """{"channel":"android-prd-global","version":"$build.0.0","build":"$build","updateUrl":"${site.origin}/releases/#v$build.0.0"}"""
        } else {
          "[]"
        }
        return HttpResponseData(
          if (fail) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK,
          GMTDate(),
          Headers.Empty,
          HttpProtocolVersion.HTTP_1_1,
          ByteReadChannel(body),
          callContext(),
        )
      }
    }
    client = HttpClient(engine)
    KoinPlatform.getKoin().declare(client, qualifier = named("publicUpdates"))
    KoinPlatform.getKoin().declare(site)
    entry = KoinPlatform.getKoin().get()
    compose.setContent {
      ComposeAppTheme {
        Text("Application content")
        if (aboutVisible) entry.Screen({}, {}, {}, {}, {})
        entry.UpdateHandler(visible, releaseNotesVersion, { releaseNotesVersion = null })
      }
    }
  }

  @After fun stop() {
    stopKoin()
    client.close()
  }

  private fun foreground() = compose.runOnIdle { entry.onForegroundChanged(true) }
  private fun background() = compose.runOnIdle { entry.onForegroundChanged(false) }
  private fun awaitRequests(count: Int) = compose.waitUntil(10_000) { requests.get() == count }
  private fun awaitOffer() = compose.waitUntil(10_000) {
    compose.onAllNodes(hasText("Notes $build.0.0 is available.")).fetchSemanticsNodes().isNotEmpty()
  }

  @Test fun startupAndForegroundShareChecksAndDismissedBuildsStaySilent() {
    foreground()
    awaitRequests(1)
    awaitOffer()
    foreground()
    compose.runOnIdle { assertEquals(1, requests.get()) }
    compose.onNodeWithText("Got it").performClick()
    background()
    foreground()
    awaitRequests(2)
    compose.waitForIdle()
    compose.onNode(isDialog()).assertDoesNotExist()
    compose.runOnIdle { aboutVisible = true }
    compose.onNodeWithText("Check", substring = false).performScrollTo().performClick()
    awaitRequests(3)
    awaitOffer()
    compose.onNodeWithText("Got it").performClick()
    compose.runOnIdle { build = 3 }
    background()
    foreground()
    awaitRequests(4)
    awaitOffer()
  }

  @Test fun automaticFailureAndCurrentVersionStaySilentAndRetryOnForeground() {
    fail = true
    foreground()
    awaitRequests(1)
    compose.waitForIdle()
    compose.onNode(isDialog()).assertDoesNotExist()
    compose.runOnIdle {
      fail = false
      build = 1
    }
    background()
    foreground()
    awaitRequests(2)
    compose.waitForIdle()
    compose.onNode(isDialog()).assertDoesNotExist()
    compose.runOnIdle { build = 2 }
    background()
    foreground()
    awaitOffer()
  }

  @Test fun releaseNotesHavePriorityAndMissingNotesDoNotBlockUpdateOffers() {
    compose.runOnIdle { releaseNotesVersion = "1.1.0" }
    foreground()
    compose.waitUntil(10_000) {
      compose.onAllNodes(hasText("What’s new in 1.1.0")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("Notes 2.0.0 is available.").assertDoesNotExist()
    compose.onNodeWithText("Got it").performClick()
    awaitOffer()
    compose.runOnIdle { releaseNotesVersion = "99.0.0" }
    awaitOffer()
  }

  @Test fun manualCheckJoinsAutomaticRequestAndShowsFailureAndRetry() {
    responseGate = CompletableDeferred()
    fail = true
    foreground()
    awaitRequests(1)
    compose.runOnIdle { aboutVisible = true }
    compose.onNodeWithText("Check", substring = false).performScrollTo().performClick()
    compose.runOnIdle {
      assertEquals(1, requests.get())
      responseGate.complete(Unit)
    }
    compose.waitUntil(10_000) {
      compose.onAllNodes(hasText("Could not check for updates. Try again.")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.runOnIdle { fail = false }
    compose.onNodeWithText("Retry").performClick()
    awaitRequests(2)
    awaitOffer()
  }

  @Test fun activeCheckIsNotDuplicatedAndDialogWaitsForHostReadiness() {
    responseGate = CompletableDeferred()
    compose.runOnIdle { visible = false }
    foreground()
    awaitRequests(1)
    background()
    foreground()
    compose.runOnIdle {
      assertEquals(1, requests.get())
      responseGate.complete(Unit)
    }
    compose.waitForIdle()
    compose.onNode(isDialog()).assertDoesNotExist()
    compose.runOnIdle { visible = true }
    awaitOffer()
  }
}
