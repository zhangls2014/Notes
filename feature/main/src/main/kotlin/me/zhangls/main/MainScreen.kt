package me.zhangls.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.zhangls.main.home.FavoritesScreen
import me.zhangls.main.home.HomeScreen
import me.zhangls.settings.SettingsResult
import me.zhangls.settings.SettingsScreen

/**
 * @author zhangls
 */
private enum class MainTab(val label: Int, val icon: ImageVector) {
  HOME(R.string.main_label_home, Icons.Rounded.Home),
  FAVORITES(R.string.main_label_favorites, Icons.Rounded.Favorite),
  SETTINGS(R.string.main_label_settings, Icons.Rounded.Settings),
}

/**
 * 判断当前 [NavigationSuiteScaffold] 类型是否是底部导航栏
 */
private fun isBottomNavigationBar(type: NavigationSuiteType): Boolean {
  val navigationBarArray = arrayOf(
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar
  )
  return navigationBarArray.contains(type)
}

/**
 * 根据导航类型创建合适的 Pager
 */
@Composable
private fun NavigationPager(
  isBottomNavigationBar: Boolean,
  pagerState: PagerState,
  pageContent: @Composable (page: Int) -> Unit
) {
  if (isBottomNavigationBar) {
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(onResult: (MainResult) -> Unit) {
  val adaptiveInfo = currentWindowAdaptiveInfo()
  val scope = rememberCoroutineScope()
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { MainTab.entries.size })
  val customLayoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
  val isBottomNavigationBar = remember(customLayoutType) { isBottomNavigationBar(customLayoutType) }
  val viewmodel: EmailViewModel = hiltViewModel()

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
    val pageContent = @Composable { page: Int ->
      when (MainTab.entries[page]) {
        MainTab.HOME -> HomeScreen(isBottomNavigationBar = isBottomNavigationBar, viewmodel = viewmodel)
        MainTab.FAVORITES -> {
          FavoritesScreen(isBottomNavigationBar = isBottomNavigationBar, viewmodel = viewmodel) { emailId ->
            onResult(MainResult.NavigateToEmailDetail(emailId))
          }
        }

        MainTab.SETTINGS -> SettingsScreen { result ->
          if (result == SettingsResult.Logout) {
            onResult(MainResult.Logout)
          }
        }
      }
    }

    NavigationPager(
      isBottomNavigationBar = isBottomNavigationBar,
      pagerState = pagerState,
      pageContent = pageContent
    )
  }
}

@Preview
@Composable
private fun MainContentPreview() {
  MainScreen(onResult = {})
}
