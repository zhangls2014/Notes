package me.zhangls.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.login.api.LoginDestination
import me.zhangls.login.api.LoginEntry
import me.zhangls.login.api.LoginResult
import org.koin.compose.koinInject

/**
 * 登录页的导航条目。
 *
 * [onLoginResult] 除了结果本身，还把**登录页自己的 key**一并交回。登录成功的去向就存在这个
 * key 的 [LoginDestination.redirectTo] 里，调用方直接读它即可 —— 不需要宿主另存一份"待跳转
 * 目标"的组合状态（那份状态活不过 Activity 重建）。
 *
 * key 与它的序列化登记都在契约模块（[me.zhangls.login.api.LoginNavigation]）。
 */
fun EntryProviderScope<NavKey>.loginNavEntry(
  onLoginResult: (result: LoginResult, destination: LoginDestination) -> Unit,
) {
  entry<LoginDestination> { destination ->
    koinInject<LoginEntry>().Screen(
      onLoginResult = { result -> onLoginResult(result, destination) },
    )
  }
}
