package me.zhangls.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import io.ktor.http.URLBuilder
import me.zhangls.about.api.NotesWebsite
import me.zhangls.theme.component.CenteredTopAppBar
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_history
import notes.feature.about.generated.resources.about_release_history_load_failed
import notes.feature.about.generated.resources.about_update_retry
import notes.feature.about.generated.resources.about_website
import notes.feature.about.generated.resources.about_website_load_failed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReleaseHistoryScreen(onBack: () -> Unit, site: NotesWebsite) {
  val appearance = currentNoticesAppearance()
  val url = remember(site, appearance) {
    URLBuilder("${site.origin}/releases/").apply {
      parameters.append("embed", "1")
      parameters.append("platform", releasePlatform)
      appearance.noticesAppearanceParameters().forEach { (name, value) -> parameters.append(name, value) }
    }.buildString()
  }
  AboutWebPage(
    url = url,
    title = stringResource(Res.string.about_release_history),
    errorMessage = stringResource(Res.string.about_release_history_load_failed),
    onBack = onBack,
    allowSameOriginNavigation = false,
  )
}

@Composable
internal fun OfficialWebsiteScreen(onBack: () -> Unit, site: NotesWebsite) {
  val appearance = currentNoticesAppearance()
  val url = remember(site, appearance) {
    URLBuilder("${site.origin.trimEnd('/')}/").apply {
      appearance.noticesAppearanceParameters().forEach { (name, value) -> parameters.append(name, value) }
    }.buildString()
  }
  AboutWebPage(
    url = url,
    title = stringResource(Res.string.about_website),
    errorMessage = stringResource(Res.string.about_website_load_failed),
    onBack = onBack,
    allowSameOriginNavigation = true,
  )
}

@Composable
private fun AboutWebPage(
  url: String,
  title: String,
  errorMessage: String,
  onBack: () -> Unit,
  allowSameOriginNavigation: Boolean,
) {
  val session = remember(url) { NoticesWebSession(url) }
  val state = session.state
  val openInBrowser = rememberOpenInBrowser()
  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(
      title = title,
      navigate = onBack,
      actions = { WebPageRefreshButton(onClick = session::refresh) },
    )
  }) { padding ->
    Box(Modifier.fillMaxSize().padding(padding.webViewContentPadding(direction))) {
      if (state.failed) {
        AboutWebPageError(errorMessage, onRetry = session::refresh, modifier = Modifier.align(Alignment.Center))
      } else {
        key(state) {
          NoticesWebView(
            url = url,
            state = state,
            projectPage = false,
            onOpenDetail = { link, _ -> link.projectUrl?.let(openInBrowser) },
            initialScroll = 0.0,
            bottomInset = padding.calculateBottomPadding(),
            modifier = Modifier.fillMaxSize(),
            allowSameOriginNavigation = allowSameOriginNavigation,
          )
        }
        if (state.loading) LinearProgressIndicator(Modifier.align(Alignment.TopCenter))
      }
    }
  }
}

@Composable
private fun AboutWebPageError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(message)
    TextButton(onClick = onRetry) { Text(stringResource(Res.string.about_update_retry)) }
  }
}
