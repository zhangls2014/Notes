package me.zhangls.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.delay
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.api.OnlineNoticeDetailDestination
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Close
import me.zhangls.theme.icon.Icons
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_close_notice_detail
import notes.feature.about.generated.resources.about_project_load_failed
import notes.feature.about.generated.resources.about_project_source
import notes.feature.about.generated.resources.about_notice_detail
import notes.feature.about.generated.resources.about_notices_load_failed
import notes.feature.about.generated.resources.about_open_source_notices
import notes.feature.about.generated.resources.about_update_retry
import org.jetbrains.compose.resources.stringResource

private const val PROGRESS_DISPLAY_DELAY_MILLIS = 250L

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OnlineNoticesPage(
  site: NotesWebsite,
  detail: OnlineNoticeDetailDestination?,
  onBack: () -> Unit,
  onOpenDetail: (OnlineNoticeDetailDestination) -> Unit,
) {
  val anchor = detail?.anchor
  val project = detail?.projectUrl
  val appearance = currentNoticesAppearance()
  val url = remember(site, appearance, detail) {
    if (project != null) return@remember project
    val filename = if (anchor == null) "$releasePlatform.html" else "$releasePlatform-licenses.html"
    URLBuilder("${site.origin}/open-source/$filename").apply {
      if (anchor != null && noticeAnchor.matches(anchor)) fragment = anchor
      appearance.noticesAppearanceParameters().forEach { (name, value) -> parameters.append(name, value) }
    }.buildString()
  }
  var attempt by remember { mutableIntStateOf(0) }
  val state = remember(url, attempt) { NoticesWebState() }
  var listScroll by rememberSaveable { mutableStateOf(0.0) }
  val listVisible = LocalListDetailSceneScope.current?.scaffoldTransitionScope?.scaffoldStateTransition
    ?.targetState?.get(ListDetailPaneScaffoldRole.List)
  val showClose = detail != null && listVisible != null && listVisible != PaneAdaptedValue.Hidden
  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(
      title = stringResource(
        when {
          project != null -> Res.string.about_project_source
          detail != null -> Res.string.about_notice_detail
          else -> Res.string.about_open_source_notices
        },
      ),
      navigate = onBack.takeUnless { showClose },
      actions = {
        if (showClose) {
          IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.about_close_notice_detail))
          }
        }
      },
    )
  }) { padding ->
    Box(
      Modifier.fillMaxSize().padding(
        start = padding.calculateStartPadding(direction),
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(direction),
      ),
    ) {
      if (state.failed) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(stringResource(if (project == null) Res.string.about_notices_load_failed else Res.string.about_project_load_failed))
          TextButton(onClick = { attempt++ }) { Text(stringResource(Res.string.about_update_retry)) }
        }
      } else {
        key(state) {
          NoticesWebView(
            url = url,
            state = state,
            projectPage = project != null,
            onOpenDetail = { selected, scroll ->
              if (detail == null) listScroll = scroll
              onOpenDetail(selected)
            },
            initialScroll = listScroll,
            bottomInset = padding.calculateBottomPadding(),
            modifier = Modifier.fillMaxSize(),
          )
        }
        if (state.loading) {
          var showProgress by remember(state) { mutableStateOf(false) }
          LaunchedEffect(state) {
            delay(PROGRESS_DISPLAY_DELAY_MILLIS)
            showProgress = true
          }
          if (showProgress) LinearProgressIndicator(Modifier.align(Alignment.TopCenter))
        }
      }
    }
  }
}

internal class NoticesWebState {
  var loading by mutableStateOf(true)
  var failed by mutableStateOf(false)

  fun fail() {
    failed = true
  }
}

internal sealed interface NoticesLink {
  data object InPage : NoticesLink
  data class Project(val url: String) : NoticesLink
  data object Blocked : NoticesLink
  data class Detail(val anchor: String) : NoticesLink
}

private val noticeAnchor = Regex("(?:license|notice)-[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*")

internal fun noticesLink(target: String, initialUrl: String, projectPage: Boolean = false): NoticesLink {
  val hasWebScheme = target.startsWith("https://", ignoreCase = true) || target.startsWith("http://", ignoreCase = true)
  if (!hasWebScheme || target.any { it.isISOControl() || it == '\\' }) {
    return NoticesLink.Blocked
  }
  val url = runCatching { Url(target) }.getOrNull() ?: return NoticesLink.Blocked
  if (url.host.isBlank() || url.user != null || url.password != null) return NoticesLink.Blocked
  if (projectPage) return NoticesLink.InPage
  val initial = Url(initialUrl)
  if (url.protocol == URLProtocol.HTTPS && url.host.equals(initial.host, ignoreCase = true) &&
    url.port == initial.port
  ) {
    if (url.encodedPath == "/open-source/$releasePlatform-licenses.html") {
      if (!noticeAnchor.matches(url.fragment)) return NoticesLink.Blocked
      return if (url.encodedPath == initial.encodedPath && url.fragment == initial.fragment) {
        NoticesLink.InPage
      } else {
        NoticesLink.Detail(url.fragment)
      }
    }
    if (url.encodedPath == initial.encodedPath) return NoticesLink.InPage
    if (url.encodedPath == "/open-source/$releasePlatform.html") return NoticesLink.Blocked
  }
  return NoticesLink.Project(target)
}

@Composable
internal expect fun NoticesWebView(
  url: String,
  state: NoticesWebState,
  projectPage: Boolean,
  onOpenDetail: (OnlineNoticeDetailDestination, Double) -> Unit,
  initialScroll: Double,
  bottomInset: Dp,
  modifier: Modifier,
)
