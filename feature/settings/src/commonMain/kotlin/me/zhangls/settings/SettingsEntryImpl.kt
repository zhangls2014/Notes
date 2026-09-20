package me.zhangls.settings

import androidx.compose.runtime.Composable
import me.zhangls.settings.api.SettingsEntry
import me.zhangls.settings.api.SettingsResult
import org.koin.core.annotation.Singleton

/**
 * [SettingsEntry] 的实现：以内联方式渲染设置页（作为主界面的一个 Tab）。
 *
 * 通过 Koin 绑定到 [SettingsEntry] 接口。兄弟 feature（如 main）只依赖 `:feature:settings-api`，
 * 运行时由本实现提供，从而切断 feature 间的直接依赖；仅 `app`（组合根）依赖 `:feature:settings`。
 *
 * @author zhangls
 */
@Singleton(binds = [SettingsEntry::class])
class SettingsEntryImpl : SettingsEntry {
  @Composable
  override fun Screen(
    onResult: (SettingsResult) -> Unit,
  ) {
    SettingsScreen(
      onResult = onResult,
    )
  }
}
