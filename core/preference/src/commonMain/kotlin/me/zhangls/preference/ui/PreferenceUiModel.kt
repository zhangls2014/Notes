package me.zhangls.preference.ui

import me.zhangls.preference.PreferenceSpec

/**
 * 设置项的**表现层模型** = 静态展示元数据（[PreferenceSpec]）+ 当前取值 + 取值变化要做什么。
 *
 * 消费方的全部工作就是装配这个模型（`SettingsModel` → `List<PreferenceUiModel>`）并交给
 * [PreferenceRow] 渲染；它不再需要知道某个 spec 该怎么画。
 *
 * 这个类型放在 `core:preference` 而不是某个 feature，是因为它属于**渲染契约**的一半：
 * [PreferenceSpec] 描述"长什么样"，本类型描述"取什么值、变了通知谁"，两者合起来才是
 * [PreferenceRow] / `SelectIconButton` 的完整入参。把取值与回调整形留在 feature 里，
 * 结果是每个消费方都要重写一遍 spec → 控件的粘合代码。
 *
 * 三条约束：
 * ① 回调一律 `(T) -> Unit` —— 消费方的 Intent 类型（如 `SettingsIntent`）**不得**出现在
 *    本模块；它是 `feature:settings` 的实现细节，带进来立刻形成 impl → impl 依赖环
 *    （`SettingsIntent` 定义在 feature 的 impl 模块内，不在 `-api`）；
 * ② 不持有"取值从哪来" —— 取值由消费方从自己的 State 里读出后传入，本模块不认识 DataStore
 *    也不认识任何仓库；
 * ③ 不做相等性承诺 —— 属性里含 lambda，`data class` 的 `equals` 对函数按引用比较，
 *    两个语义等价的模型并不相等。需要去重 / 比较时请用 `spec.key`。
 *
 * @author zhangls
 */
sealed interface PreferenceUiModel {
  val spec: PreferenceSpec

  /** 开关型设置项。 */
  data class Toggle(
    override val spec: PreferenceSpec.Toggle,
    val value: Boolean,
    val onValueChange: (Boolean) -> Unit,
  ) : PreferenceUiModel

  /** 单值选择型设置项。 */
  data class Select<T>(
    override val spec: PreferenceSpec.Select<T>,
    val value: T,
    val onValueChange: (T) -> Unit,
  ) : PreferenceUiModel

  /** 点击型设置项（如"退出登录"）。 */
  data class Action(
    override val spec: PreferenceSpec.Action,
    val onClick: () -> Unit,
  ) : PreferenceUiModel
}
