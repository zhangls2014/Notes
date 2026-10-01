package me.zhangls.settings.api

import androidx.compose.runtime.Composable

/**
 * Settings 功能对外暴露的入口契约。
 *
 * 兄弟 feature 只依赖本接口（`:feature:settings-api`），具体实现由 `:feature:settings`
 * 通过 Koin 提供并绑定到该接口，仅 `app`（组合根）依赖实现模块。以此切断 feature 间的直接依赖。
 *
 * 内容内边距不由本契约传递：导航套件占用的那部分系统内边距由应用外壳消费掉
 * （`AppShell` 的 `consumeWindowInsets`），页面直接用 `Scaffold` 给出的 padding。
 * 契约里因此没有布局细节 —— 既没有"导航在哪一侧"，也没有窗口尺寸。
 *
 * @author zhangls
 */
interface SettingsEntry {
  /**
   * 以内联方式渲染设置页（作为主界面的一个 Tab，而非独立导航目的地）。
   *
   * @param onResult 设置页产生的对外结果回调，例如 [SettingsResult.Logout]
   */
  @Composable
  fun Screen(
    onResult: (SettingsResult) -> Unit,
  )
}
