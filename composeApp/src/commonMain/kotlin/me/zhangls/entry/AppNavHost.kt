package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.plus
import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.email.emailNavEntry
import me.zhangls.email.emailNavModule
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.NavEffect.Restart
import me.zhangls.framework.nav.RequireLogin
import me.zhangls.login.LoginDestination
import me.zhangls.login.api.LoginResult
import me.zhangls.login.loginNavEntry
import me.zhangls.login.loginNavModule
import me.zhangls.main.AppShell
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.main.mainNavEntries
import me.zhangls.main.mainNavModule

/**
 * @author zhangls
 */
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

  // 返回栈的序列化配置。remember 住：每次重组新建一个 SerializersModule 毫无意义，
  // 还会让 rememberNavBackStack 依赖的配置对象持续变化。
  val config = remember {
    SavedStateConfiguration {
      serializersModule = mainNavModule + loginNavModule + emailNavModule
    }
  }

  // 首帧传进来的 DeepLink 直接作为返回栈的初始内容。NavDisplay 进入组合的瞬间即
  // require(entries.isNotEmpty())，给初始元素比"先建空栈、再在组合期往里补"更稳，
  // 也免掉了组合期写返回栈这段副作用。
  // 注意初始内容同样要过登录守卫 —— 它是首屏，不是"已经过检查的栈内容"。
  val initialDeepLink = remember { deepLinkDestination }
  val backStack = rememberNavBackStack(
    configuration = config,
    *initialBackStack(initialDeepLink, isLogin).toTypedArray(),
  )

  // navHandler 会被传进 NavDisplay 的 entryProvider，而 rememberDecoratedNavEntries 按返回栈
  // 内容缓存已创建的 entry —— 旧 entry 里捕获到的是**创建那一刻**的 navHandler。因此这里既
  // remember 住实例（避免无谓重建），又用 rememberUpdatedState 让它读到的始终是当前登录态，
  // 而不是某个历史帧的快照。
  val currentIsLogin by rememberUpdatedState(isLogin)
  val navHandler = remember(backStack) {
    NavHandler(backStack = backStack, isLogin = { currentIsLogin })
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

  // 外壳（Rail / 底部导航栏）是 NavDisplay 的容器，不是返回栈里的一个条目：
  // 它读返回栈的**根**决定选中哪个 Tab，因此推入详情页时导航套件依然可见、可达，
  // 宽屏上不会从"列表 + 详情"突然变成全屏页。根是登录页时不显示外壳。
  AppShell(
    selected = backStack.tabSelection(),
    onSelectTab = { navHandler(Restart(it)) },
  ) {
    NavDisplay(
      backStack = backStack,
      // 每个 NavEntry 一个 ViewModelStore：ViewModel 随 entry 一起创建、随 entry 出栈一起清除。
      // 少了这一项，NavDisplay 只挂 SaveableStateHolder，所有 ViewModel 都会落到宿主（Activity）
      // 的 store 里，于是登录页的 ViewModel 永不 onCleared（明文密码残留），详情页与列表页也会
      // 共用同一个实例。
      // 顺序不能反：SaveableStateHolder 装饰器负责提供 SavedStateRegistryOwner，
      // ViewModelStore 装饰器要读它才能给每个 entry 的 ViewModel 装配 SavedStateHandle。
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
      // 转场与 onBack 都走 NavDisplay 的默认值：默认转场是**按平台**给的 —— Android 是
      // Material 的 fade（predictive back 走 spring + scaleOut），iOS 是 500ms 原生曲线
      // 配合 veil/unveil。手写一套线性横滑盖在两端之上，等于把两个平台各自的原生观感一起丢掉。
      entryProvider = entryProvider {
        mainNavEntries(
          navigateToEmailDetail = { emailId ->
            navHandler(NavEffect.Navigate(EmailDetailDestination(emailId)))
          },
          onLogout = { navHandler(Restart(LoginDestination())) },
        )

        emailNavEntry { effect -> navHandler(effect) }

        loginNavEntry { result, destination ->
          if (result == LoginResult.Success) {
            // 去向取自登录页 key 自身携带的 redirectTo：它随返回栈一起被序列化恢复，
            // 因此 Activity 重建后依然成立；为空则进主页。
            // isLogin 显式传 true —— 此刻 AppViewModel 里的登录态可能还没回调到位。
            navHandler(NavEffect.Replace(destination.redirectTo ?: HomeDestination), isLogin = true)
          }
        }
      },
    )
  }
}

/**
 * 返回栈的初始内容。
 *
 * 起始 Tab（[HomeDestination]）恒为栈底：它既是"返回的终点"（在别的 Tab 上按返回先回起始
 * Tab，而不是直接退出应用 —— 这是 Android 上"返回"的普遍预期），也是外壳判断选中项的依据。
 * 因此非 Tab 的目的地（如 DeepLink 直达的详情页）一律垫在它之上，不能自己当栈底。
 *
 * 未登录时被拦下的目标不占栈位，而是写进登录页 key 的 `redirectTo` —— 它随返回栈一起被序列化
 * 恢复，所以首屏 DeepLink 的目标在 Activity 重建后依然成立。
 */
private fun initialBackStack(
  deepLink: DeepLinkDestination?,
  isLogin: Boolean,
): List<NavKey> = when {
  !isLogin -> listOf(LoginDestination(redirectTo = deepLink?.takeIf { it is RequireLogin }))
  deepLink == null || deepLink == HomeDestination -> listOf(HomeDestination)
  else -> listOf(HomeDestination, deepLink)
}

/**
 * 当前选中的 Tab：返回栈里**最后一个** Tab 目的地。
 *
 * 不取栈顶是因为栈顶可能是压在 Tab 之上的页面（详情页），此时选中项应当保持为它下面的 Tab。
 * 栈里没有 Tab（根是登录页）时返回 `null`，外壳不显示。
 */
private fun List<NavKey>.tabSelection(): TabDestination? =
  lastOrNull { it is TabDestination } as? TabDestination

private class NavHandler(
  private val backStack: NavBackStack<NavKey>,
  private val isLogin: () -> Boolean,
) {
  operator fun invoke(effect: NavEffect, isLogin: Boolean? = null) {
    backStack.handle(effect, isLogin ?: this.isLogin())
  }

  private fun NavBackStack<NavKey>.handle(effect: NavEffect, isLogin: Boolean) {
    when (effect) {
      is NavEffect.Navigate -> push(effect.dest, isLogin)

      is NavEffect.Replace -> {
        removeLastOrNull()
        push(effect.dest, isLogin)
      }

      is Restart -> {
        clear()
        push(effect.dest, isLogin)
      }

      // 只弹一层。栈只剩根时系统返回根本不会进到这里 —— NavDisplay 的 isBackEnabled 取自
      // scene.previousEntries，栈只有一项时它是 false，事件直接交还系统（退出应用）。
      NavEffect.Popup -> if (size > 1) removeLastOrNull()
    }
  }

  private fun NavBackStack<NavKey>.push(target: Destination, isLogin: Boolean) {
    val destination = target.guardedByLogin(isLogin)
    // 幂等：栈顶已是同一目标就不再压栈。少了这一条，快速连点同一个列表项会压入两个相同的
    // key，用户"按一次返回"没有反应，得按两次才回到上一页。
    // 栈空时先垫栈底 —— Replace / Restart 都可能把栈清空，而外壳需要栈里有 Tab。
    if (isEmpty()) add(rootFor(destination))
    if (lastOrNull() != destination) add(destination)
  }
}

/**
 * 栈底的选择。
 *
 * 起始 Tab 与登录页本身就是栈底；其他目的地（别的 Tab、详情页）一律垫在起始 Tab 之上 ——
 * 这样"在非起始 Tab 上按返回先回起始 Tab"就是返回栈自身的语义，宿主不需要拦截返回键。
 *
 * 这里不需要判断登录态：未登录时 [guardedByLogin] 已把需要登录的目标换成登录页，
 * 而登录页正是它自己该当的栈底。
 */
private fun rootFor(destination: Destination): Destination = when (destination) {
  is LoginDestination -> destination
  is HomeDestination -> destination
  else -> HomeDestination
}

/**
 * 未登录时访问需要登录的目标，改为落回登录页，并把原始目标写进登录页的 key。
 *
 * 待跳转目标必须跟着 key 走而不是待在组合内存里：key 会被 `rememberNavBackStack` 连同整个
 * 返回栈一起序列化并恢复，而组合内存活不过 Activity 重建。
 *
 * 返回栈的**初始内容**与之后每一次压栈共用这一个判断。首屏 DeepLink 尤其不能漏 —— 它是
 * 未登录时最容易到达受限页面的入口（外部链接直开）。
 */
private fun Destination.guardedByLogin(isLogin: Boolean): Destination {
  return if (this is RequireLogin && !isLogin) LoginDestination(redirectTo = this) else this
}
