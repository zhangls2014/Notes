package me.zhangls.settings.ui

import me.zhangls.preference.PreferenceSpec
import me.zhangls.settings.mvi.SettingsIntent

/**
 * 设置项的表现层模型 = 静态展示元数据（[PreferenceSpec]，来自 `core:preference`）
 * + 当前取值 + 取值变化要做什么（两者都只属于本模块）。
 *
 * 拆成这两半是有意的，不能合并回一个类型：
 * - 元数据跨 feature 共享（登录页也用语言 / 深色两项），必须放 core 层；
 * - 当前取值来自 State，行为是 [SettingsIntent] —— 后者定义在**本 impl 模块**内
 *   （不在 `:feature:settings-api`），一旦把它塞进 `core:preference` 就会形成
 *   impl → impl 的依赖环。
 *
 * @author zhangls
 */
sealed interface PreferenceUiModel {
  val spec: PreferenceSpec

  data class Toggle(
    override val spec: PreferenceSpec.Toggle,
    val value: Boolean,
    val onValueChange: (Boolean) -> SettingsIntent,
  ) : PreferenceUiModel

  data class Select<T>(
    override val spec: PreferenceSpec.Select<T>,
    val value: T,
    val onValueChange: (T) -> SettingsIntent,
  ) : PreferenceUiModel

  data class Action(
    override val spec: PreferenceSpec.Action,
    val clickIntent: SettingsIntent,
  ) : PreferenceUiModel
}
