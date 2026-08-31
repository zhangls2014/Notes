package me.zhangls.main.api

import androidx.compose.runtime.Composable

/**
 * Main 功能对外暴露的入口契约。
 *
 * 兄弟 feature 只依赖本接口（`:feature:main-api`），具体实现由 `:feature:main`
 * 通过 Koin 提供并绑定到该接口，仅 `app`（组合根）依赖实现模块。以此切断 feature 间的直接依赖。
 *
 * @author zhangls
 */
interface MainEntry {
  /**
   * 渲染主界面（作为独立导航目的地，由导航装配调用）。
   *
   * @param onResult 主界面产生的对外结果回调，例如 [MainResult.Logout]
   */
  @Composable
  fun Screen(onResult: (MainResult) -> Unit = {})
}
