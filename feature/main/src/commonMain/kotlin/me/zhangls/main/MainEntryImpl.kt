package me.zhangls.main

import androidx.compose.runtime.Composable
import me.zhangls.main.api.MainEntry
import me.zhangls.main.api.MainResult
import org.koin.core.annotation.Singleton

/**
 * [MainEntry] 的实现：独立渲染主界面（首页 / 收藏 / 设置三个 Tab）。
 *
 * 通过 Koin 绑定到 [MainEntry] 接口。兄弟 feature 只依赖 `:feature:main-api`，
 * 运行时由本实现提供，从而切断 feature 间的直接依赖；仅 `app`（组合根）依赖 `:feature:main`。
 *
 * @author zhangls
 */
@Singleton(binds = [MainEntry::class])
class MainEntryImpl : MainEntry {
  @Composable
  override fun Screen(onResult: (MainResult) -> Unit) {
    MainScreen(onResult = onResult)
  }
}
