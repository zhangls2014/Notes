package me.zhangls.preference

import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.theme.ThemeColor
import org.jetbrains.compose.resources.StringResource

/**
 * 单个选项：展示文案与实际取值。
 *
 * 与 [PreferenceSpec.Select] 的消费方无关，取值类型由使用方（通常是 `core:model` 里的设置值枚举）决定。
 *
 * @author zhangls
 */
data class PreferenceOption<T>(
  val label: StringResource,
  val value: T,
)

/**
 * 设置项的**静态展示元数据**：一个设置项"长什么样、叫什么"，不含它当前的取值，也不含取值变化后要做什么。
 *
 * 本类型是 `core:preference` 词表的一半，另一半是渲染契约 `ui/PreferenceUiModel`（元数据 + 取值 + 回调），
 * 两者配成一对交给 `ui/` 里的控件。三条硬约束：
 * ① **不含行为** —— 当前取值、`onValueChange` / 点击回调、以及消费方自己的 Intent 类型都不在此；
 *    取值与回调整形由消费方装进 `ui/PreferenceUiModel`，签名一律 `(T) -> Unit`，故 feature 类型不会回流；
 * ② **只管"长什么样"，不管"怎么画"** —— 本类型不引用任何渲染 API。怎么画由本模块 `ui/` 的控件决定
 *    （[PreferenceRow] 行形态 / [SelectIconButton] 图标形态），同一份 spec 可以有多种形态，
 *    每种形态在本模块实现一次；
 * ③ **不含清单与顺序** —— 哪些设置项存在、以什么顺序展示是**平台相关**的
 *    （动态取色仅 Android 有），因此清单在 feature 的 common 中统一定义，expect/actual 只提供平台能力位。
 *
 * 元数据对象在各自的 `object`（如 [LanguagePreference]）中声明为单例，故 lambda 属性按引用参与相等性
 * 不产生实际影响。
 *
 * @author zhangls
 */
sealed interface PreferenceSpec {
  /** 稳定的标识，用作列表的 key（不要依赖标题文案）。 */
  val key: String

  /** 设置项标题。 */
  val title: StringResource

  /**
   * 设置项图标。
   *
   * 取非空是有意的：展示元数据的消费者（登录页的图标按钮、设置页的列表项）都要求有图标，
   * 可空会逼它们写永远走不到的空分支。若将来真出现无图标的设置项，再把对应变体放宽为可空。
   */
  val icon: ImageVector

  /**
   * 开关型设置项的元数据。
   *
   * @property summary 由当前取值推导的摘要文案；返回 null 表示不显示摘要
   */
  data class Toggle(
    override val key: String,
    override val title: StringResource,
    override val icon: ImageVector,
    val summary: (Boolean) -> StringResource? = { null },
  ) : PreferenceSpec

  /**
   * 单值选择型设置项（下拉 / 列表选择）的元数据。
   *
   * @property options 可选项，顺序即展示顺序
   */
  data class Select<T>(
    override val key: String,
    override val title: StringResource,
    override val icon: ImageVector,
    val options: List<PreferenceOption<T>>,
  ) : PreferenceSpec {
    /**
     * 当前取值的摘要文案 —— 直接取对应选项的 [PreferenceOption.label]。
     *
     * 这是有意的**单一来源**：选项文案只写一遍，摘要不再维护第二份 `when`。
     * 代价是失去 `when` 的穷尽性检查 —— 给取值枚举新增常量时**必须**同步补上对应的
     * [PreferenceOption]，否则该项既不出现在选项列表里、也没有摘要（不报错，只是静默缺失）。
     *
     * 取值不在 [options] 中时返回 null。
     */
    fun summary(value: T): StringResource? = options.firstOrNull { it.value == value }?.label
  }

  /**
   * 点击型设置项的元数据（如"退出登录"）。
   *
   * 注意这里**没有**点击后要做的事 —— 那是消费方的 Intent，不在元数据里。
   *
   * @property tint 图标与标题的着色语义，null 表示用主题默认色
   */
  data class Action(
    override val key: String,
    override val title: StringResource,
    override val icon: ImageVector,
    val summary: StringResource? = null,
    val tint: ThemeColor? = null,
  ) : PreferenceSpec
}
