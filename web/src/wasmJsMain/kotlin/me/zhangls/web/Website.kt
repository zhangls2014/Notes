package me.zhangls.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Check
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.contentWidth
import me.zhangls.theme.layout.rememberWindowAdaptiveInfo
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.android
import me.zhangls.web.resources.ios
import me.zhangls.web.resources.notes_icon
import me.zhangls.web.resources.noto_sans_sc
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.painterResource

private enum class Page(val url: String) {
  Home("/"), Download("/downloads/"), Releases("/releases/"), Notices("/open-source/")
}

private data class Route(val page: Page, val platform: String, val version: String? = null) {
  fun url(): String = when (page) {
    Page.Releases -> page.url + "?platform=$platform" + (version?.let { "#v$it" } ?: "")
    Page.Notices -> page.url + "?platform=$platform"
    else -> page.url
  }
}

private fun routeFromBrowser(): Route = Route(
  page = Page.entries.firstOrNull { it.url.trimEnd('/') == path().trimEnd('/') } ?: Page.Home,
  platform = parameter("platform")?.takeIf { it == "android" || it == "ios" } ?: browserPlatform(),
  version = fragment().takeIf { it.startsWith("#v") }?.drop(2),
)

private inline fun <reified T : Enum<T>> saved(key: String, default: T): T =
  enumValues<T>().firstOrNull { it.name == preference(key) } ?: default

private val siteJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  configureWebResources { resourcePathMapping { "/$it" } }
  val data = siteJson.decodeFromString<SiteData>(siteData())
  val container = checkNotNull(document.getElementById("notes-app"))
  ComposeViewport(container) {
    Website(data)
    SideEffect { hideFallback() }
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun Website(data: SiteData) {
  var route by remember { mutableStateOf(routeFromBrowser()) }
  var releaseResult by remember { mutableStateOf<Result<List<Release>>?>(null) }
  var releaseAttempt by remember { mutableStateOf(0) }
  LaunchedEffect(route.page, releaseAttempt) {
    if (route.page == Page.Releases && releaseResult == null) {
      releaseResult = try {
        Result.success(siteJson.decodeFromString<List<Release>>(releaseJson()))
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        Result.failure(failure)
      }
    }
  }
  var language by remember {
    mutableStateOf(
      when (parameter("lang")?.substringBefore('-')?.lowercase()) {
        "zh" -> AppLanguage.CHINESE
        "en" -> AppLanguage.ENGLISH
        else -> saved("language", AppLanguage.FOLLOW_SYSTEM)
      }
    )
  }
  var theme by remember {
    mutableStateOf(
      when (parameter("theme")) {
        "dark" -> DarkThemeConfig.DARK
        "light" -> DarkThemeConfig.LIGHT
        else -> saved("theme", DarkThemeConfig.FOLLOW_SYSTEM)
      }
    )
  }
  var font by remember { mutableStateOf(saved("fontSize", FontSizeConfig.STANDARD)) }
  var explicitScale by remember { mutableStateOf(parameter("fontScale")?.toFloatOrNull()?.takeIf { it in .5f..5f }) }
  var systemDark by remember { mutableStateOf(browserDark()) }
  val embedded = parameter("embed") == "1"
  val colors = remember {
    EmbedColors(
      background = appearanceColor("bg"),
      card = appearanceColor("card"),
      foreground = appearanceColor("fg"),
      muted = appearanceColor("muted"),
      outline = appearanceColor("line"),
      accent = appearanceColor("accent"),
      container = appearanceColor("container"),
      onContainer = appearanceColor("on-container"),
    )
  }
  val chinese = when (language) {
    AppLanguage.CHINESE -> true
    AppLanguage.ENGLISH -> false
    AppLanguage.FOLLOW_SYSTEM -> browserLanguage().startsWith("zh", ignoreCase = true)
  }
  val dark = theme == DarkThemeConfig.DARK || theme == DarkThemeConfig.FOLLOW_SYSTEM && systemDark
  val scale = explicitScale ?: font.value
  val localized = if (chinese) "zh" else "en"
  fun navigate(next: Route) {
    if (next == route) return
    pushPage(next.url())
    route = next
  }
  remember {
    onHistoryChange { route = routeFromBrowser() }
    onThemeChange { systemDark = browserDark() }
  }
  SideEffect { displayLanguage(if (chinese) "zh-Hans" else "en") }

  ComposeAppTheme(darkTheme = dark, fontScale = scale, fontFamily = FontFamily(Font(Res.font.noto_sans_sc))) {
    val base = MaterialTheme.colorScheme
    val scheme = base.copy(
      background = colors.background ?: base.background,
      surface = colors.background ?: base.surface,
      surfaceContainerLow = colors.card ?: base.surfaceContainerLow,
      onSurface = colors.foreground ?: base.onSurface,
      onSurfaceVariant = colors.muted ?: base.onSurfaceVariant,
      outlineVariant = colors.outline ?: base.outlineVariant,
      primary = colors.accent ?: base.primary,
      primaryContainer = colors.container ?: base.primaryContainer,
      onPrimaryContainer = colors.onContainer ?: base.onPrimaryContainer,
    )
    MaterialTheme(colorScheme = scheme) {
      Surface(Modifier.fillMaxSize(), color = scheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          val wide = maxWidth >= 760.dp * scale
          val wideNavigation = maxWidth >= 620.dp * scale
          val compactHeading = maxWidth < 200.dp * scale
          val inset = if (wide) 40.dp else 16.dp
          Column(Modifier.contentWidth(ContentWidth.Prose + ContentWidth.Form).padding(horizontal = inset)) {
            if (!embedded) {
              WebsiteNavigation(
                route, chinese, language, theme, font, wideNavigation, ::navigate,
                onLanguage = {
                  language = it
                  storePreference("language", it.name)
                  clearParameter("lang")
                },
                onTheme = {
                  theme = it
                  storePreference("theme", it.name)
                  clearParameter("theme")
                },
                onFont = {
                  font = it
                  explicitScale = null
                  storePreference("fontSize", it.name)
                  clearParameter("fontScale")
                },
              )
            }
            val details = releaseResult?.getOrNull().orEmpty().filter { release -> release.changes.any { route.platform in it.platforms } }
            val selected = details.firstOrNull { it.version == route.version } ?: details.firstOrNull().takeIf { wide }
            val detailRoute = selected?.let { route.copy(version = it.version) }
            val backStack = if (route.page == Page.Releases && detailRoute != null) {
              listOf(route.copy(version = null), detailRoute)
            } else {
              listOf(route.copy(version = null))
            }
            val directive = calculatePaneScaffoldDirective(rememberWindowAdaptiveInfo()).let {
              it.copy(maxHorizontalPartitions = if (wide) 2 else 1)
            }
            val strategy = rememberListDetailSceneStrategy<Route>(directive = directive)
            val paneIsWide by rememberUpdatedState(wide)
            val useCompactHeading by rememberUpdatedState(compactHeading)
            val useChinese by rememberUpdatedState(chinese)
            val appearanceQuery by rememberUpdatedState(
              "lang=$localized&theme=${if (dark) "dark" else "light"}&fontScale=$scale"
            )
            NavDisplay(
              backStack = backStack,
              modifier = Modifier.fillMaxSize(),
              onBack = { if (route.page == Page.Releases) navigate(route.copy(version = null)) },
              sceneStrategies = listOf(strategy),
              entryProvider = { entry ->
                val metadata = when {
                  entry.page != Page.Releases -> emptyMap()
                  entry.version == null -> ListDetailSceneStrategy.listPane()
                  else -> ListDetailSceneStrategy.detailPane()
                }
                NavEntry(entry, metadata = metadata) {
                  Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    when (entry.page) {
                      Page.Home -> HomePage(useChinese, paneIsWide, useCompactHeading, onDownload = { navigate(Route(Page.Download, route.platform)) },
                        onReleases = { navigate(Route(Page.Releases, route.platform)) })
                      Page.Download -> DownloadPage(useChinese, data.builds, useCompactHeading)
                      Page.Notices -> NoticesPage(useChinese, entry.platform) { noticePath ->
                        openPage("$noticePath?$appearanceQuery")
                      }
                      Page.Releases -> {
                        val result = releaseResult
                        val releases = result?.getOrNull().orEmpty()
                        val platformReleases = remember(releases, entry.platform) {
                          releases.filter { release -> release.changes.any { entry.platform in it.platforms } }
                        }
                        when {
                          result == null -> Text(if (useChinese) "正在加载更新日志…" else "Loading release notes…")
                          result.isFailure -> {
                            Text(if (useChinese) "无法加载更新日志。" else "Could not load release notes.")
                            TextButton(onClick = {
                              releaseResult = null
                              releaseAttempt++
                            }) {
                              Text(if (useChinese) "重试" else "Retry")
                            }
                          }
                          entry.version != null -> {
                            if (!paneIsWide) TextButton(onClick = { navigate(route.copy(version = null)) }) {
                              Text(if (useChinese) "返回版本列表" else "Back to versions")
                            }
                            releases.firstOrNull { it.version == entry.version }?.let { release ->
                              ReleaseCard(release, entry.platform, useChinese)
                            }
                          }
                          platformReleases.isEmpty() -> Text(if (useChinese) "暂无已发布记录。" else "No published releases yet.")
                          paneIsWide -> platformReleases.forEach { release ->
                            TextButton(onClick = { navigate(route.copy(version = release.version)) }) {
                              Text(release.version + "  ·  " + release.date)
                            }
                          }
                          else -> platformReleases.forEach { release ->
                            ReleaseCard(release, entry.platform, useChinese) {
                              navigate(route.copy(version = release.version))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              },
            )
          }
        }
      }
    }
  }
}

private data class EmbedColors(
  val background: Color?, val card: Color?, val foreground: Color?, val muted: Color?,
  val outline: Color?, val accent: Color?, val container: Color?, val onContainer: Color?,
)

private fun appearanceColor(key: String): Color? = parameter(key)?.takeIf {
  it.length == 6 && it.all { digit -> digit.digitToIntOrNull(16) != null }
}?.toIntOrNull(16)?.let { Color(it or (0xFF shl 24)) }

@Composable
private fun WebsiteNavigation(
  route: Route,
  chinese: Boolean,
  language: AppLanguage,
  theme: DarkThemeConfig,
  font: FontSizeConfig,
  wide: Boolean,
  navigate: (Route) -> Unit,
  onLanguage: (AppLanguage) -> Unit,
  onTheme: (DarkThemeConfig) -> Unit,
  onFont: (FontSizeConfig) -> Unit,
) {
  val brand: @Composable () -> Unit = {
    TextButton(onClick = { navigate(Route(Page.Home, route.platform)) }) {
      Image(
        painter = painterResource(Res.drawable.notes_icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall),
      )
      Text(if (chinese) "摘星" else "Notes", Modifier.padding(start = 8.dp))
    }
  }
  val links: @Composable () -> Unit = {
    if (route.page != Page.Releases) {
      TextButton(onClick = { navigate(Route(Page.Download, route.platform)) }) {
        Text(if (chinese) "下载" else "Downloads")
      }
    }
    PlatformMenu(
      if (chinese) "更新日志" else "Release notes",
      route.platform.takeIf { route.page == Page.Releases },
    ) { navigate(Route(Page.Releases, it)) }
    PlatformMenu(
      if (chinese) "开源声明" else "Open-source notices",
      route.platform.takeIf { route.page == Page.Notices },
    ) { navigate(Route(Page.Notices, it)) }
  }
  val actions: @Composable () -> Unit = {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp, if (wide) Alignment.End else Alignment.Start),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      links()
      SettingsMenu(chinese, language, theme, font, wide, onLanguage, onTheme, onFont)
    }
  }
  if (wide) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      brand()
      Spacer(Modifier.weight(1f))
      actions()
    }
  } else {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
      brand()
      actions()
    }
  }
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PlatformMenu(label: String, selected: String?, onSelect: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    val trigger: @Composable RowScope.() -> Unit = {
      Text(label, Modifier.weight(1f, fill = false))
      Icon(
        Icons.Rounded.ArrowBackIosNew,
        contentDescription = null,
        modifier = Modifier.padding(start = 6.dp).size(16.dp).rotate(if (expanded) 90f else -90f),
      )
    }
    if (selected == null) {
      TextButton(onClick = { expanded = true }, content = { trigger() })
    } else {
      FilledTonalButton(onClick = { expanded = true }, content = { trigger() })
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.widthIn(min = 160.dp),
      shape = MaterialTheme.shapes.large,
    ) {
      listOf("android" to "Android", "ios" to "iOS").forEach { (platform, name) ->
        DropdownMenuItem(
          text = { Text(name) },
          modifier = if (selected == platform) {
            Modifier.background(MaterialTheme.colorScheme.secondaryContainer).semantics { this.selected = true }
          } else {
            Modifier
          },
          leadingIcon = {
            val icon = if (platform == "android") Res.drawable.android else Res.drawable.ios
            Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp))
          },
          trailingIcon = if (selected == platform) ({ Icon(Icons.Rounded.Check, null) }) else null,
          onClick = { expanded = false; onSelect(platform) },
        )
      }
    }
  }
}

@Composable
private fun SettingsMenu(
  chinese: Boolean,
  language: AppLanguage,
  theme: DarkThemeConfig,
  font: FontSizeConfig,
  wide: Boolean,
  onLanguage: (AppLanguage) -> Unit,
  onTheme: (DarkThemeConfig) -> Unit,
  onFont: (FontSizeConfig) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val system = if (chinese) "跟随系统" else "System"
  Box {
    OutlinedButton(
      onClick = { expanded = true },
      contentPadding = PaddingValues(horizontal = if (wide) 24.dp else 12.dp, vertical = 8.dp),
    ) {
      Icon(Icons.Rounded.Settings, null, Modifier.size(18.dp))
      Text(if (chinese) "设置" else "Settings", Modifier.padding(start = 8.dp))
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.widthIn(min = 196.dp),
      shape = MaterialTheme.shapes.large,
    ) {
      SettingOptions(if (chinese) "语言" else "Language", language,
        listOf(AppLanguage.FOLLOW_SYSTEM to system, AppLanguage.CHINESE to "简体中文", AppLanguage.ENGLISH to "English"),
        onLanguage,
      )
      HorizontalDivider()
      SettingOptions(if (chinese) "深色模式" else "Appearance", theme,
        listOf(DarkThemeConfig.FOLLOW_SYSTEM to system, DarkThemeConfig.LIGHT to if (chinese) "浅色" else "Light",
          DarkThemeConfig.DARK to if (chinese) "深色" else "Dark"),
        onTheme,
      )
      HorizontalDivider()
      SettingOptions(if (chinese) "字体大小" else "Text size", font,
        FontSizeConfig.entries.zip(if (chinese) listOf("标准", "中等", "大") else listOf("Standard", "Medium", "Large")),
        onFont,
      )
    }
  }
}

@Composable
private fun <T> SettingOptions(title: String, selected: T, choices: List<Pair<T, String>>, onSelect: (T) -> Unit) {
  Text(title, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
  choices.forEach { (value, label) ->
    DropdownMenuItem(
      text = { Text(label) },
      trailingIcon = if (selected == value) ({ Icon(Icons.Rounded.Check, null) }) else null,
      onClick = { onSelect(value) },
    )
  }
}
