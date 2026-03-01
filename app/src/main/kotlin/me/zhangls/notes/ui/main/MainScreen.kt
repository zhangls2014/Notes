package me.zhangls.notes.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.zhangls.notes.R
import me.zhangls.notes.ui.favorites.FavoritesScreen
import me.zhangls.notes.ui.home.HomeScreen
import me.zhangls.notes.ui.profile.ProfileScreen

/**
 * @author zhangls
 */
private enum class MainTab(val label: Int, val icon: ImageVector) {
  HOME(R.string.app_label_home, Icons.Rounded.Home),
  FAVORITES(R.string.app_label_favorites, Icons.Rounded.Favorite),
  PROFILE(R.string.app_label_settings, Icons.Rounded.AccountCircle),
}

/**
 * 判断当前 [NavigationSuiteScaffold] 类型是否是底部导航栏
 */
private fun isNavigationBar(type: NavigationSuiteType): Boolean {
  val navigationBarArray = arrayOf(
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar
  )
  return navigationBarArray.contains(type)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(onMainResult: (MainResult) -> Unit) {
  val adaptiveInfo = currentWindowAdaptiveInfo()
  val scope = rememberCoroutineScope()
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { MainTab.entries.size })
  val customLayoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
  val isNavigationBar = remember(customLayoutType) {
    mutableStateOf(isNavigationBar(customLayoutType))
  }

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      MainTab.entries.forEach {
        item(
          icon = { Icon(imageVector = it.icon, contentDescription = stringResource(it.label)) },
          label = { Text(stringResource(it.label)) },
          selected = pagerState.currentPage == MainTab.entries.indexOf(it),
          onClick = {
            val currentTab = MainTab.entries.indexOf(it)
            scope.launch { pagerState.animateScrollToPage(currentTab) }
          }
        )
      }
    },
    layoutType = customLayoutType,
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
      userScrollEnabled = false
    ) {
      when (MainTab.entries[it]) {
        MainTab.HOME -> HomeScreen(
          isNavigationBar = isNavigationBar.value,
          onClick = { onMainResult(MainResult.Logout) })

        MainTab.FAVORITES -> FavoritesScreen()
        MainTab.PROFILE -> ProfileScreen()
      }
    }
  }
}

@Preview
@Composable
private fun MainContentPreview() {
  MainScreen(onMainResult = {})
}