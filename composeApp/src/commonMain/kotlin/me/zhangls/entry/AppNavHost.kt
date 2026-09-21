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
import me.zhangls.main.MainDestination
import me.zhangls.main.api.MainResult
import me.zhangls.main.mainNavEntry
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

  // 首帧传进来的 DeepLink 直接作为返回栈的初始元素。NavDisplay 进入组合的瞬间即
  // require(backStack.isNotEmpty())，给初始元素比"先建空栈、再在组合期往里补一个"更稳，
  // 也免掉了组合期写返回栈这段副作用。
  // 注意初始元素同样要过登录守卫 —— 它是首屏，不是"已经过检查的栈内容"。
  val initialDeepLink = remember { deepLinkDestination }
  val initialDestination = (initialDeepLink ?: if (isLogin) MainDestination else LoginDestination())
    .guardedByLogin(isLogin)
  val backStack = rememberNavBackStack(config, initialDestination)

  // navHandler 会被传进 NavDisplay 的 entryProvider，而 rememberDecoratedNavEntries 按返回栈
  // 内容缓存已创建的 entry —— 旧 entry 里捕获到的是**创建那一刻**的 navHandler。因此这里既
  // remember 住实例（避免无谓重建），又用 rememberUpdatedState 让它读到的始终是当前登录态，
  // 而不是某个历史帧的快照。
  val currentIsLogin by rememberUpdatedState(isLogin)
  val navHandler = remember(backStack) {
    NavHandler(backStack = backStack, isLogin = { currentIsLogin })
  }

  // 运行期 DeepLink：事件驱动，放 effect 里执行，避免重组重复触发导航。
  // 首帧的 DeepLink 已并入返回栈的初始元素，所以只有"与首帧不同的那一个"才是新来的。
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
    // 配合 veil/unveil。此前这里手写了一套线性横滑盖在两端之上，等于把两个平台各自的原生
    // 观感一起丢掉，且 pop 与 predictivePop 两份内容完全重复。
    entryProvider = entryProvider {
      mainNavEntry { result ->
        when (result) {
          MainResult.Logout -> {
            navHandler(Restart(LoginDestination()))
          }

          is MainResult.NavigateToEmailDetail -> {
            navHandler(
              NavEffect.Navigate(EmailDetailDestination(result.emailId)),
            )
          }
        }
      }

      loginNavEntry { result, destination ->
        if (result == LoginResult.Success) {
          // 去向取自登录页 key 自身携带的 redirectTo：它随返回栈一起被序列化恢复，
          // 因此 Activity 重建后依然成立；为空则进主页。
          // isLogin 显式传 true —— 此刻 AppViewModel 里的登录态可能还没回调到位。
          navHandler(NavEffect.Replace(destination.redirectTo ?: MainDestination), isLogin = true)
        }
      }

      emailNavEntry {
        navHandler(it)
      }
    }
  )
}


private class NavHandler(
  private val backStack: NavBackStack<NavKey>,
  private val isLogin: () -> Boolean,
) {
  operator fun invoke(effect: NavEffect, isLogin: Boolean? = null) {
    backStack.handle(effect, isLogin ?: this.isLogin())
  }

  private fun NavBackStack<NavKey>.handle(effect: NavEffect, isLogin: Boolean) {
    when (effect) {
      is NavEffect.Navigate -> {
        // 幂等：栈顶已是同一目标就不再压栈。
        // 少了这一条，快速连点同一个列表项会压入两个相同的 key，用户"按一次返回"没有反应，
        // 得按两次才回到上一页（A/B 实测：无此判断时连点两次后第 1 次返回仍停在详情页）。
        val target = effect.dest.guardedByLogin(isLogin)
        if (lastOrNull() != target) add(target)
      }

      is NavEffect.Replace -> {
        removeLastOrNull()
        add(effect.dest.guardedByLogin(isLogin))
      }

      is Restart -> {
        clear()
        add(effect.dest.guardedByLogin(isLogin))
      }

      NavEffect.Popup -> {
        if (size > 1) {
          removeLastOrNull()
        } else {
          val root = if (isLogin) MainDestination else LoginDestination()
          if (firstOrNull() != root) {
            add(0, root)
            removeLastOrNull()
          }
        }
      }
    }
  }
}

/**
 * 未登录时访问需要登录的目标，改为落回登录页，并把原始目标写进登录页的 key。
 *
 * 待跳转目标必须跟着 key 走而不是待在组合内存里：key 会被 `rememberNavBackStack` 连同整个
 * 返回栈一起序列化并恢复，而组合内存活不过 Activity 重建。
 *
 * 返回栈的**初始元素**与之后每一次压栈共用这一个判断。首屏 DeepLink 尤其不能漏 —— 它是
 * 未登录时最容易到达受限页面的入口（外部链接直开）。
 */
private fun Destination.guardedByLogin(isLogin: Boolean): Destination {
  return if (this is RequireLogin && !isLogin) LoginDestination(redirectTo = this) else this
}
