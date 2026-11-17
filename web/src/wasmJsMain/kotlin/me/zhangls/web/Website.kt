package me.zhangls.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
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
import me.zhangls.theme.icon.Code
import me.zhangls.theme.icon.Download
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.icon.ViewAgenda
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.contentWidth
import me.zhangls.theme.layout.rememberWindowAdaptiveInfo
import me.zhangls.web.notices.NoticeDocument
import me.zhangls.web.notices.hasLegacyNoticePalette
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.android
import me.zhangls.web.resources.app_name
import me.zhangls.web.resources.appearance
import me.zhangls.web.resources.back_to_home
import me.zhangls.web.resources.back_to_versions
import me.zhangls.web.resources.dark
import me.zhangls.web.resources.downloads
import me.zhangls.web.resources.ios
import me.zhangls.web.resources.language
import me.zhangls.web.resources.language_english
import me.zhangls.web.resources.language_simplified_chinese
import me.zhangls.web.resources.light
import me.zhangls.web.resources.notes_icon
import me.zhangls.web.resources.noto_sans_sc
import me.zhangls.web.resources.open_source_notices
import me.zhangls.web.resources.page_not_found
import me.zhangls.web.resources.release_notes
import me.zhangls.web.resources.releases_empty
import me.zhangls.web.resources.releases_failure
import me.zhangls.web.resources.releases_loading
import me.zhangls.web.resources.retry
import me.zhangls.web.resources.settings
import me.zhangls.web.resources.system
import me.zhangls.web.resources.text_large
import me.zhangls.web.resources.text_medium
import me.zhangls.web.resources.text_size
import me.zhangls.web.resources.text_standard
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.painterResource

private enum class Page(val url: String) {
  Home("/"), Download("/downloads/"), Releases("/releases/"), Notices("/open-source/"),
  NoticeDocument(""), NotFound("/404.html");

  val standalone: Boolean get() = this == NoticeDocument || this == NotFound
}

private data class Route(val page: Page, val platform: String, val version: String? = null) {
  fun url(): String = when (page) {
    Page.Releases -> page.url + "?platform=$platform" + (version?.let { "#v$it" } ?: "")
    Page.Notices -> page.url + "?platform=$platform"
    else -> page.url
  }
}

private fun routeFromBrowser(): Route {
  val documentPlatform = if (document.getElementById("notes-notice-data") != null) {
    path().substringAfterLast('/').substringBefore('-').substringBefore('.')
  } else {
    null
  }
  return Route(
    page = when {
      documentPlatform != null -> Page.NoticeDocument
      document.getElementById("notes-app")?.hasAttribute("data-not-found") == true -> Page.NotFound
      else -> Page.entries.firstOrNull { !it.standalone && it.url.trimEnd('/') == path().trimEnd('/') }
        ?: Page.NotFound
    },
    platform = documentPlatform ?: parameter("platform")?.takeIf { it == "android" || it == "ios" } ?: browserPlatform(),
    version = fragment().takeIf { it.startsWith("#v") }?.drop(2),
  )
}

private inline fun <reified T : Enum<T>> saved(key: String, default: T): T =
  enumValues<T>().firstOrNull { it.name == preference(key) } ?: default

private val siteJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  configureWebResources { resourcePathMapping { "/$it" } }
  val data = siteData()?.let { siteJson.decodeFromString<SiteData>(it) } ?: SiteData(emptyList())
  val container = checkNotNull(document.getElementById("notes-app"))
  ComposeViewport(container) {
    Website(data)
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
  val embedded = parameter("embed") == "1" || (route.page == Page.NoticeDocument && hasLegacyNoticePalette())
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
  val requestedChinese = when (language) {
    AppLanguage.CHINESE -> true
    AppLanguage.ENGLISH -> false
    AppLanguage.FOLLOW_SYSTEM -> browserLanguage().startsWith("zh", ignoreCase = true)
  }
  val strings by produceState<WebsiteStrings?>(initialValue = null, requestedChinese) {
    value = WebsiteStrings.load(requestedChinese)
  }
  val resolvedStrings = strings ?: return
  val chinese = resolvedStrings.chinese
  val dark = theme == DarkThemeConfig.DARK || theme == DarkThemeConfig.FOLLOW_SYSTEM && systemDark
  val scale = explicitScale ?: font.value
  fun openNoticeList(platform: String) {
    openPage(siteTarget("/open-source/$platform.html"))
  }
  fun navigate(next: Route) {
    if (next == route) return
    val target = siteTarget(next.url())
    if (route.page.standalone) {
      openPage(target)
      return
    }
    pushPage(target)
    route = next
  }
  remember {
    onHistoryChange { route = routeFromBrowser() }
    onThemeChange { systemDark = browserDark() }
  }
  SideEffect {
    displayLanguage(if (chinese) "zh-Hans" else "en")
    if (route.page == Page.NotFound) {
      document.title = "${resolvedStrings[Res.string.page_not_found]} · ${resolvedStrings[Res.string.app_name]}"
    }
  }

  CompositionLocalProvider(LocalWebsiteStrings provides resolvedStrings) {
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
            val siteMaxWidth = ContentWidth.Prose + ContentWidth.Form
            val topNavigationMinWidth = if (chinese) 620.dp else 740.dp
            val wideNavigation = minOf(maxWidth, siteMaxWidth) >= topNavigationMinWidth * maxOf(1f, scale)
            val railWidth = if (!embedded && !wideNavigation) {
              (maxWidth * 0.16f).coerceIn(48.dp, 96.dp)
            } else {
              null
            }
            val contentWidth = maxWidth - (railWidth ?: 0.dp)
            val compactHeading = contentWidth < 200.dp * scale
            val inset = if (wide) 40.dp else 16.dp
            val siteWidth = Modifier.contentWidth(siteMaxWidth).padding(horizontal = inset)
            val navigationWidth = Modifier.contentWidth(siteMaxWidth).padding(horizontal = 16.dp)
            val navigation: @Composable () -> Unit = {
              WebsiteNavigation(
                route, language, theme, font, railWidth, ::navigate,
                onOpenNotices = ::openNoticeList,
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
            Row(Modifier.fillMaxSize()) {
              if (railWidth != null) navigation()
              Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) siteColumn@{
                if (!embedded && wideNavigation) Column(navigationWidth) { navigation() }
                if (route.page == Page.NoticeDocument) {
                  NoticeDocument(resolvedStrings, route.platform)
                  return@siteColumn
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
                NavDisplay(
                  backStack = backStack,
                  modifier = siteWidth.fillMaxSize(),
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
                          Page.Home -> HomePage(paneIsWide, useCompactHeading, onDownload = { navigate(Route(Page.Download, route.platform)) },
                            onReleases = { navigate(Route(Page.Releases, route.platform)) })
                          Page.Download -> DownloadPage(data.builds, useCompactHeading)
                          Page.Notices -> NoticesPage(entry.platform) { openNoticeList(entry.platform) }
                          Page.NoticeDocument -> Unit // Rendered directly to retain the notice's own scroll viewport.
                          Page.NotFound -> {
                            Text("404", style = MaterialTheme.typography.labelLarge)
                            Text(websiteString(Res.string.page_not_found), style = MaterialTheme.typography.headlineLarge)
                            FilledTonalButton(onClick = { navigate(Route(Page.Home, route.platform)) }) {
                              Text(websiteString(Res.string.back_to_home))
                            }
                          }
                          Page.Releases -> {
                            val result = releaseResult
                            val releases = result?.getOrNull().orEmpty()
                            val platformReleases = remember(releases, entry.platform) {
                              releases.filter { release -> release.changes.any { entry.platform in it.platforms } }
                            }
                            when {
                              result == null -> Text(websiteString(Res.string.releases_loading))
                              result.isFailure -> {
                                Text(websiteString(Res.string.releases_failure))
                                TextButton(onClick = {
                                  releaseResult = null
                                  releaseAttempt++
                                }) {
                                  Text(websiteString(Res.string.retry))
                                }
                              }
                              entry.version != null -> {
                                if (!paneIsWide) TextButton(onClick = { navigate(route.copy(version = null)) }) {
                                  Text(websiteString(Res.string.back_to_versions))
                                }
                                releases.firstOrNull { it.version == entry.version }?.let { release ->
                                  ReleaseCard(release, entry.platform, useChinese)
                                }
                              }
                              platformReleases.isEmpty() -> Text(websiteString(Res.string.releases_empty))
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
  }
}

private data class EmbedColors(
  val background: Color?, val card: Color?, val foreground: Color?, val muted: Color?,
  val outline: Color?, val accent: Color?, val container: Color?, val onContainer: Color?,
)

internal fun appearanceColor(key: String): Color? = parameter(key)?.takeIf {
  it.length == 6 && it.all { digit -> digit.digitToIntOrNull(16) != null }
}?.toIntOrNull(16)?.let { Color(it or (0xFF shl 24)) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebsiteNavigation(
  route: Route,
  language: AppLanguage,
  theme: DarkThemeConfig,
  font: FontSizeConfig,
  railWidth: Dp?,
  navigate: (Route) -> Unit,
  onOpenNotices: (String) -> Unit,
  onLanguage: (AppLanguage) -> Unit,
  onTheme: (DarkThemeConfig) -> Unit,
  onFont: (FontSizeConfig) -> Unit,
) {
  val wide = railWidth == null
  val itemSpacing = 8.dp
  val goHome = { navigate(Route(Page.Home, route.platform)) }
  val goToDownloads = { navigate(Route(Page.Download, route.platform)) }
  val downloadLabel = websiteString(Res.string.downloads)
  val releaseMenu: @Composable () -> Unit = {
    PlatformMenu(
      websiteString(Res.string.release_notes),
      route.platform.takeIf { route.page == Page.Releases },
      railIcon = if (wide) null else Icons.Rounded.ViewAgenda,
    ) { navigate(Route(Page.Releases, it)) }
  }
  val noticeMenu: @Composable () -> Unit = {
    PlatformMenu(
      websiteString(Res.string.open_source_notices),
      route.platform.takeIf { route.page == Page.Notices || route.page == Page.NoticeDocument },
      railIcon = if (wide) null else Icons.Rounded.Code,
      onSelect = onOpenNotices,
    )
  }

  if (wide) {
    TopAppBar(
      title = {
        TextButton(onClick = goHome) {
          Image(painterResource(Res.drawable.notes_icon), null, Modifier.size(24.dp))
          Text(websiteString(Res.string.app_name), Modifier.padding(start = 8.dp))
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      actions = {
        Row(
          horizontalArrangement = Arrangement.spacedBy(itemSpacing),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TopNavigationButton(selected = route.page == Page.Download, onClick = goToDownloads) {
            Text(downloadLabel)
          }
          releaseMenu()
          noticeMenu()
          SettingsMenu(language, theme, font, wide, onLanguage, onTheme, onFont)
        }
      },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
  } else {
    NavigationRail(
      modifier = Modifier.width(railWidth),
      header = {
        NavigationRailItem(
          selected = route.page == Page.Home,
          onClick = goHome,
          icon = { Image(painterResource(Res.drawable.notes_icon), websiteString(Res.string.app_name), Modifier.size(24.dp)) },
        )
      },
    ) {
      Column(
        Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
      ) {
        NavigationRailItem(
          selected = route.page == Page.Download,
          onClick = goToDownloads,
          icon = { Icon(Icons.Rounded.Download, downloadLabel) },
        )
        releaseMenu()
        noticeMenu()
        SettingsMenu(language, theme, font, wide, onLanguage, onTheme, onFont)
      }
    }
  }
}

@Composable
private fun TopNavigationButton(
  selected: Boolean,
  onClick: () -> Unit,
  content: @Composable RowScope.() -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  TextButton(
    onClick = onClick,
    modifier = Modifier.semantics { this.selected = selected },
    colors = ButtonDefaults.textButtonColors(
      containerColor = if (selected) colors.secondaryContainer else Color.Transparent,
      contentColor = if (selected) colors.onSecondaryContainer else colors.primary,
    ),
    content = content,
  )
}

@Composable
private fun PlatformMenu(
  label: String,
  selected: String?,
  railIcon: ImageVector?,
  onSelect: (String) -> Unit,
) {
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
    if (railIcon != null) {
      NavigationRailItem(
        selected = selected != null,
        onClick = { expanded = true },
        icon = { Icon(railIcon, label) },
      )
    } else {
      TopNavigationButton(selected = selected != null, onClick = { expanded = true }, content = trigger)
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
  language: AppLanguage,
  theme: DarkThemeConfig,
  font: FontSizeConfig,
  wide: Boolean,
  onLanguage: (AppLanguage) -> Unit,
  onTheme: (DarkThemeConfig) -> Unit,
  onFont: (FontSizeConfig) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val system = websiteString(Res.string.system)
  Box {
    if (wide) {
      OutlinedButton(
        onClick = { expanded = true },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
      ) {
        Icon(Icons.Rounded.Settings, null, Modifier.size(18.dp))
        Text(websiteString(Res.string.settings), Modifier.padding(start = 8.dp))
      }
    } else {
      IconButton(onClick = { expanded = true }) {
        Icon(Icons.Rounded.Settings, websiteString(Res.string.settings))
      }
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.widthIn(min = 196.dp),
      shape = MaterialTheme.shapes.large,
    ) {
      SettingOptions(websiteString(Res.string.language), language,
        listOf(
          AppLanguage.FOLLOW_SYSTEM to system,
          AppLanguage.CHINESE to websiteString(Res.string.language_simplified_chinese),
          AppLanguage.ENGLISH to websiteString(Res.string.language_english),
        ),
        onLanguage,
      )
      HorizontalDivider()
      SettingOptions(websiteString(Res.string.appearance), theme,
        listOf(
          DarkThemeConfig.FOLLOW_SYSTEM to system,
          DarkThemeConfig.LIGHT to websiteString(Res.string.light),
          DarkThemeConfig.DARK to websiteString(Res.string.dark),
        ),
        onTheme,
      )
      HorizontalDivider()
      SettingOptions(websiteString(Res.string.text_size), font,
        FontSizeConfig.entries.zip(listOf(
          websiteString(Res.string.text_standard),
          websiteString(Res.string.text_medium),
          websiteString(Res.string.text_large),
        )),
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
