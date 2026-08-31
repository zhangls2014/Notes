package me.zhangls.login

import androidx.compose.runtime.Composable
import me.zhangls.login.api.LoginEntry
import me.zhangls.login.api.LoginResult
import org.koin.core.annotation.Singleton

/**
 * [LoginEntry] 的实现：独立渲染登录页。
 *
 * 通过 Koin 绑定到 [LoginEntry] 接口。兄弟 feature 只依赖 `:feature:login-api`，
 * 运行时由本实现提供，从而切断 feature 间的直接依赖；仅 `app`（组合根）依赖 `:feature:login`。
 *
 * @author zhangls
 */
@Singleton(binds = [LoginEntry::class])
class LoginEntryImpl : LoginEntry {
  @Composable
  override fun Screen(onLoginResult: (LoginResult) -> Unit) {
    LoginScreen(onLoginResult = onLoginResult)
  }
}
