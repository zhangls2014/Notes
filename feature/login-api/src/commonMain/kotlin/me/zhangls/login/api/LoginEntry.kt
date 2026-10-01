package me.zhangls.login.api

import androidx.compose.runtime.Composable

/**
 * Login 功能对外暴露的入口契约。
 *
 * 兄弟 feature 只依赖本接口（`:feature:login-api`），具体实现由 `:feature:login`
 * 通过 Koin 提供并绑定到该接口，仅 `app`（组合根）依赖实现模块。以此切断 feature 间的直接依赖。
 *
 * @author zhangls
 */
interface LoginEntry {
  /**
   * 渲染登录页（作为独立导航目的地，由导航装配调用）。
   *
   * @param onLoginResult 登录页产生的对外结果回调，例如 [LoginResult.Success]
   */
  @Composable
  fun Screen(onLoginResult: (LoginResult) -> Unit)
}
