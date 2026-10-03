package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.about.aboutNavEntry
import me.zhangls.about.api.AboutDestination
import me.zhangls.entry.util.AppInfo
import me.zhangls.entry.util.toAboutAppInfo
import org.koin.compose.koinInject
import me.zhangls.email.emailNavEntry
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.NavEffect.Restart
import me.zhangls.login.api.LoginDestination
import me.zhangls.login.api.LoginResult
import me.zhangls.login.loginNavEntry
import me.zhangls.main.AppShell
import me.zhangls.main.api.HomeDestination
import me.zhangls.profile.profileNavEntry
import me.zhangls.profile.api.ProfileDestination
import me.zhangls.main.mainNavEntries
import me.zhangls.theme.layout.ProvideModalViewport
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavHost(
  viewModel: AppViewModel,
  deepLinkDestination: DeepLinkDestination? = null,
  onDeepLinkConsumed: () -> Unit = {},
) {
  // 登录状态
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 是否登录（从 state 派生，避免组合期写状态）
  val isLogin = state.isLogin ?: false

  // 登录状态未知，不显示 UI
  if (state.isLogin == null) {
    return
  }

  // 各 feature 以唯一 qualifier 登记 key 的序列化注册表与 DeepLink 匹配器，Registry 统一收集。
  // 具体 Nav Entry、宿主回调与根导航策略仍由 composeApp 显式装配。
  val navigationRegistry = rememberNavigationRegistry()
  val platformAppInfo = koinInject<AppInfo>()
  val aboutAppInfo = remember(platformAppInfo) { platformAppInfo.toAboutAppInfo() }

  // 返回栈的序列化配置。remember 住：每次重组新建一个 SerializersModule 毫无意义，
  // 还会让 rememberNavBackStack 依赖的配置对象持续变化。
  // 漏登记某个 key 的后果是重建时 SerializationException（编译期无提示），
  // NavBackStackSerializationTest 用同一份注册表做往返守卫。
  val config = remember(navigationRegistry) {
    SavedStateConfiguration {
      serializersModule = navigationRegistry.navModule
    }
  }

  // 首帧传进来的 DeepLink 直接作为返回栈的初始内容。NavDisplay 进入组合的瞬间即
  // require(entries.isNotEmpty())，给初始元素比"先建空栈、再在组合期往里补"更稳，
  // 也免掉了组合期写返回栈这段副作用。
  // 注意初始内容同样要过登录守卫 —— 它是首屏，不是"已经过检查的栈内容"。
  val initialDeepLink = remember { deepLinkDestination }
  val navigationState = rememberAppNavigationState(config, initialBackStack(initialDeepLink, isLogin))
  val backStack = navigationState.backStack

  // navHandler 会被传进 NavDisplay 的 entryProvider，而 rememberDecoratedNavEntries 按返回栈
  // 内容缓存已创建的 entry —— 旧 entry 里捕获到的是**创建那一刻**的 navHandler。因此这里既
  // remember 住实例（避免无谓重建），又用 rememberUpdatedState 让它读到的始终是当前登录态，
  // 而不是某个历史帧的快照。
  val currentIsLogin by rememberUpdatedState(isLogin)
  val navHandler = remember(navigationState) {
    NavHandler(state = navigationState, isLogin = { currentIsLogin })
  }

  // 运行期 DeepLink：事件驱动，放 effect 里执行，避免重组重复触发导航。
  // 首帧的 DeepLink 已并入返回栈的初始内容，所以只有"与首帧不同的那一个"才是新来的。
  // 这里用引用比较而非 equals：同一条 URL 再次打开也应当重新导航。
  // 另外 MainActivity 在 Activity 重建时会重新从 intent 取 URL，此时它会被当作"首帧"值，
  // 因此不会被误判成新导航而重复压栈。
  LaunchedEffect(deepLinkDestination) {
    val destination = deepLinkDestination ?: return@LaunchedEffect
    if (destination !== initialDeepLink) {
      navHandler(NavEffect.Navigate(destination))
    }
    onDeepLinkConsumed()
  }

  val sceneStrategies = rememberAppSceneStrategies<NavKey>()
  val hasHinge = LocalWindowAdaptiveInfo.current.windowPosture.hingeList.isNotEmpty()

  // 所有栈的装饰器持续留在组合中，非活动 Entry 的保存状态及 ViewModel 因而不被清除。
  // 只把活动栈交给 NavDisplay；非活动页面不会绘制，也不会处理返回事件。
  val entriesByRoot = navigationState.backStacks.mapValues { (root, stack) ->
    key(root) {
      val decorators = listOf<NavEntryDecorator<NavKey>>(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        remember(hasHinge) {
          NavEntryDecorator { entry ->
            if (!hasHinge || LocalListDetailSceneScope.current == null) {
              entry.Content()
            } else {
              // SceneStrategy 只分配窗格位置，不会消费窗格外的系统安全区。
              // 按实际边界重算，让内侧边缘不再重复避让屏幕另一端的挖孔。
              PaneWindowInsets {
                entry.Content()
              }
            }
          }
        },
      )
      val provider = entryProvider {
        mainNavEntries(
          // 详情是返回栈里的一个真实条目，"哪封邮件正被打开"因此可以从返回栈派生。
          // 放在参数里而不是让列表自己去查：返回栈在宿主手里，列表不该知道导航结构。
          openedDetail = stack.lastOrNull() as? EmailDetailDestination,
          navigateToEmailDetail = { scene, emailId ->
            navHandler(NavEffect.Navigate(EmailDetailDestination(emailId, scene)))
          },
          navigateToProfile = { origin -> navHandler(NavEffect.Navigate(ProfileDestination(origin))) },
          navigateToAbout = { navHandler(NavEffect.Navigate(AboutDestination)) },
          onLogout = { navHandler(Restart(LoginDestination())) },
        )

        emailNavEntry { effect -> navHandler(effect) }
        profileNavEntry { effect -> navHandler(effect) }
        aboutNavEntry(aboutAppInfo) { effect -> navHandler(effect) }

        loginNavEntry { result, destination ->
          if (result == LoginResult.Success) {
            // 去向取自登录页 key 自身携带的 redirectTo：它随返回栈一起被序列化恢复，
            // 因此 Activity 重建后依然成立；为空则进主页。
            // isLogin 显式传 true —— 此刻 AppViewModel 里的登录态可能还没回调到位。
            navHandler(NavEffect.Replace(destination.redirectTo ?: HomeDestination), isLogin = true)
          }
        }
      }
      rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = decorators,
        entryProvider = provider,
      )
    }
  }

  // 外壳（Rail / 底部导航栏）是 NavDisplay 的容器，不是返回栈里的一个条目：
  // 它读返回栈的**根**决定选中哪个 Tab，因此推入详情页时导航套件依然可见、可达，
  // 宽屏上不会从"列表 + 详情"突然变成全屏页。个人信息页隐藏导航套件，但保留所属 Tab
  // 和外壳内容的组合位置；根是登录页时不显示外壳。
  ProvideModalViewport {
    TabFadeThrough(
      selected = backStack.tabSelection(),
      onSelectTab = navHandler::selectTab,
    ) { readAlpha, selectedTab, selectTab ->
      AppShell(
        selected = selectedTab,
        onSelectTab = selectTab,
        showNavigation = backStack.lastOrNull() !is ProfileDestination && backStack.lastOrNull() !is AboutDestination,
      ) {
        Box(Modifier.graphicsLayer { alpha = readAlpha() }) {
          // The old display is removed at the invisible midpoint; only the selected stack
          // owns input/back events, while outer decorators retain every Tab's Entry state.
          key(navigationState.selectedRoot) {
            DeviceCornerNavDisplay(
              entries = entriesByRoot.getValue(navigationState.selectedRoot),
              onBack = { navHandler(NavEffect.Popup) },
              sceneStrategies = sceneStrategies,
            )
          }
        }
      }
    }
  }
}
