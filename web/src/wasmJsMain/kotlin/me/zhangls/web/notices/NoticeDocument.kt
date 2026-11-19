package me.zhangls.web.notices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.theme.layout.contentWidth
import me.zhangls.web.WebsiteStrings
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.app_name
import me.zhangls.web.resources.notice_copyright
import me.zhangls.web.resources.notice_google_jetbrains
import me.zhangls.web.resources.notice_intro
import me.zhangls.web.resources.notice_licenses
import me.zhangls.web.resources.notice_not_found
import me.zhangls.web.resources.notice_other_components
import me.zhangls.web.resources.notice_project_source
import me.zhangls.web.resources.notice_scope
import me.zhangls.web.resources.notice_title
import org.w3c.dom.events.Event

@Serializable
private data class NoticeData(
  val categories: List<NoticeCategory> = emptyList(),
  val declarations: Map<String, List<String>> = emptyMap(),
)

@Serializable
private data class NoticeCategory(val id: String, val projects: List<NoticeProject>)

@Serializable
private data class NoticeProject(
  val name: String,
  val source: String,
  val modules: List<NoticeModule>,
  val declarations: List<String>,
)

@Serializable
private data class NoticeModule(val name: String, val coordinate: String)

@Composable
internal fun NoticeDocument(copy: WebsiteStrings, platform: String) {
  val filename = remember { window.location.pathname.substringAfterLast('/') }
  val platformName = if (platform == "ios") "iOS" else "Android"
  val detail = filename.endsWith("-licenses.html")
  val data = remember {
    Json.decodeFromString<NoticeData>(checkNotNull(document.getElementById("notes-notice-data")?.textContent))
  }
  val heading = copy[if (detail) Res.string.notice_licenses else Res.string.notice_title]
  var fragment by remember { mutableStateOf(window.location.hash.removePrefix("#")) }
  DisposableEffect(Unit) {
    val listener: (Event) -> Unit = { fragment = window.location.hash.removePrefix("#") }
    window.addEventListener("hashchange", listener)
    onDispose { window.removeEventListener("hashchange", listener) }
  }
  SideEffect {
    document.title = "${copy[Res.string.app_name]} · $platformName $heading"
  }

  Box(Modifier.fillMaxSize()) {
    if (detail) {
      key(fragment) { Declaration(copy, fragment, data.declarations[fragment]) }
    } else {
      Catalogue(copy, platform, platformName, heading, data.categories, fragment)
    }
  }
}

@Composable
private fun Catalogue(
  strings: WebsiteStrings,
  platform: String,
  platformName: String,
  heading: String,
  categories: List<NoticeCategory>,
  fragment: String,
) {
  val categoryPositions = remember(categories) {
    var index = 1
    categories.associate { category ->
      (category.id to index).also { index += category.projects.size + 1 }
    }
  }
  val state = rememberLazyListState()
  LaunchedEffect(fragment, categoryPositions) {
    val saved = readNoticePosition()?.split(':')
    state.scrollToItem(
      index = saved?.getOrNull(0)?.toIntOrNull() ?: categoryPositions[fragment] ?: 0,
      scrollOffset = saved?.getOrNull(1)?.toIntOrNull() ?: 0,
    )
    snapshotFlow {
      if (state.isScrollInProgress) null else state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
    }.filterNotNull()
      .collect { (index, offset) -> saveNoticePosition(index, offset) }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = state,
    contentPadding = PaddingValues(vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      NoticeContent {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.extraLarge,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(platformName, style = MaterialTheme.typography.labelLarge)
            Text(heading, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
            Text(strings[Res.string.notice_intro].replace("%1\$s", platformName))
            Text(strings[Res.string.notice_scope].replace("%1\$s", platformName))
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          categories.forEach { category ->
            FilledTonalButton(onClick = { window.location.hash = category.id }) {
              Text(category.label(strings))
            }
          }
        }
      }
    }
    categories.forEach { category ->
      item {
        NoticeContent {
          Text(category.label(strings), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
        }
      }
      items(category.projects) { project ->
        NoticeContent { ProjectCard(strings, platform, project) }
      }
    }
  }
}

@Composable
private fun ProjectCard(strings: WebsiteStrings, platform: String, project: NoticeProject) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(project.name, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
      SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          project.modules.forEach { module ->
            Text(module.name, style = MaterialTheme.typography.titleSmall)
            Text(
              module.coordinate,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        project.declarations.forEach { id ->
          val label = if (id.startsWith("license-")) id.removePrefix("license-") else strings[Res.string.notice_copyright]
          FilledTonalButton(onClick = { openNoticeLink(noticeDetailLink(platform, id)) }) { Text(label) }
        }
        FilledTonalButton(onClick = { openNoticeLink(project.source) }) { Text(strings[Res.string.notice_project_source]) }
      }
    }
  }
}

@Composable
private fun Declaration(strings: WebsiteStrings, fragment: String, texts: List<String>?) {
  Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp)) {
    NoticeContent {
      if (texts == null) {
        Text(strings[Res.string.notice_not_found])
      } else {
        val title = if (fragment.startsWith("license-")) fragment.removePrefix("license-") else strings[Res.string.notice_copyright]
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        SelectionContainer {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            texts.forEach { text ->
              Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(text, Modifier.padding(20.dp), fontFamily = FontFamily.Monospace)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NoticeContent(content: @Composable ColumnScope.() -> Unit) {
  Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
  }
}

private fun NoticeCategory.label(strings: WebsiteStrings): String =
  strings[if (id == "google-jetbrains-components") Res.string.notice_google_jetbrains else Res.string.notice_other_components]
