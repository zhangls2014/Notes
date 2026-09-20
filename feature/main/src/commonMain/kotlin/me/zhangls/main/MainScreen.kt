package me.zhangls.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.launch
import me.zhangls.email.api.EmailEntry
import me.zhangls.main.api.MainResult
import me.zhangls.settings.api.SettingsEntry
import me.zhangls.settings.api.SettingsResult
import me.zhangls.theme.icon.Favorite
import me.zhangls.theme.icon.Home
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.LocalNavigationPlacement
import me.zhangls.theme.layout.NavigationPlacement
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_label_favorites
import notes.feature.main.generated.resources.main_label_home
import notes.feature.main.generated.resources.main_label_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * @author zhangls
 */
private enum class MainTab(val label: StringResource, val icon: ImageVector) {
  HOME(Res.string.main_label_home, Icons.Rounded.Home),
  FAVORITES(Res.string.main_label_favorites, Icons.Rounded.Favorite),
  SETTINGS(Res.string.main_label_settings, Icons.Rounded.Settings),
}

/**
 * 把 [NavigationSuiteScaffold] 的布局类型归类为"导航套件占哪一侧"。
 *
 * 归类结果是宿主布局对内容区唯一的承诺，经 `LocalNavigationPlacement` 下发。
 * 原先这里是一个 `isBottomNavigationBar: Boolean`，穿透了 3 个 feature 的公开契约、
 * 在 15 个文件间手工传递 47 次，且被三处当成了三种不同含义（内边距 / 搜索栏形态 / 屏幕尺寸）。
 */
private fun NavigationSuiteType.toNavigationPlacement(): NavigationPlacement {
  val bottomNavigationTypes = arrayOf(
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar
  )
  return if (bottomNavigationTypes.contains(this)) {
    NavigationPlacement.Bottom
  } else {
    NavigationPlacement.Side
  }
}

/**
 * 根据导航方位创建合适的 Pager
 */
@Composable
private fun NavigationPager(
  placement: NavigationPlacement,
  pagerState: PagerState,
  pageContent: @Composable (page: Int) -> Unit
) {
  if (placement == NavigationPlacement.Bottom) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = false,
    ) {
      pageContent(it)
    }
  } else {
    VerticalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = false,
    ) {
      pageContent(it)
    }
  }
}

@Composable
fun MainScreen(onResult: (MainResult) -> Unit) {
  val adaptiveInfo = currentWindowAdaptiveInfo()
  val scope = rememberCoroutineScope()
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { MainTab.entries.size })
  val windowSizeClass = adaptiveInfo.windowSizeClass
  // 布局类型仅由窗口尺寸类决定，用 remember 避免每次重组重复计算
  val customLayoutType = remember(windowSizeClass) {
    with(windowSizeClass) {
      if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND)) {
        NavigationSuiteType.WideNavigationRailExpanded
      } else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        NavigationSuiteType.WideNavigationRailCollapsed
      } else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        if (minWidthDp > minHeightDp) {
          NavigationSuiteType.WideNavigationRailCollapsed
        } else {
          NavigationSuiteType.ShortNavigationBarMedium
        }
      } else {
        NavigationSuiteType.ShortNavigationBarCompact
      }
    }
  }
  val navigationPlacement = remember(customLayoutType) { customLayoutType.toNavigationPlacement() }
  val emailEntry = koinInject<EmailEntry>()
  val settingsEntry = koinInject<SettingsEntry>()

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      MainTab.entries.forEachIndexed { index, tab ->
        item(
          icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(tab.label)) },
          label = { Text(stringResource(tab.label)) },
          selected = pagerState.currentPage == index,
          onClick = {
            scope.launch { pagerState.animateScrollToPage(index) }
          }
        )
      }
    },
    layoutType = customLayoutType,
  ) {
    // 导航方位在此下发：三个 feature 的内容区据此换算内边距，
    // 契约里因此不需要任何布局参数
    CompositionLocalProvider(LocalNavigationPlacement provides navigationPlacement) {
      val pageContent = @Composable { page: Int ->
        when (MainTab.entries[page]) {
          MainTab.HOME -> emailEntry.HomeScreen()
          MainTab.FAVORITES -> emailEntry.FavoritesScreen { emailId ->
            onResult(MainResult.NavigateToEmailDetail(emailId))
          }

          MainTab.SETTINGS -> settingsEntry.Screen { result ->
            if (result == SettingsResult.Logout) {
              onResult(MainResult.Logout)
            }
          }
        }
      }

      NavigationPager(
        placement = navigationPlacement,
        pagerState = pagerState,
        pageContent = pageContent
      )
    }
  }
}

@Preview
@Composable
private fun MainContentPreview() {
  MainScreen(onResult = {})
}
