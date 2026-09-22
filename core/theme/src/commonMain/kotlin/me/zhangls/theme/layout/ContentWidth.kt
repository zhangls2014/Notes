package me.zhangls.theme.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 内容宽度上限（measure）。
 *
 * 这些是与窗口尺寸**无关**的可读性/人体工学常量，全应用集中在这里定义，而不是散落在各
 * feature 里当私有常量 —— 同一个概念在几处各写一个数字，早晚会漂移，而且没人能一眼看出
 * "这个 480 和那个 480 是不是一回事"。
 *
 * 注意它**不是**多形态适配：窗口能放几栏、导航套件该是什么形态、窗格多宽，都归
 * [LocalWindowAdaptiveInfo] 与 `PaneScaffoldDirective` 管。这里只回答"一行/一个输入框
 * 多长算合适"。
 */
object ContentWidth {
  /** 表单：两三个输入框的宽度上限，再宽也不会更好读，只会让视线来回扫。 */
  val Form = 480.dp

  /** 长正文：邮件正文一类的连续文本，超出后单行字符数超过可读范围。 */
  val Article = 720.dp

  /** 列表 / 设置行：一行里通常同时有标题、说明与控件，可以略宽一些。 */
  val Prose = 840.dp
}

/**
 * 让内容"窄窗口填满、宽窗口封顶于 [maxWidth]"。
 *
 * 顺序不能反：先 `widthIn` 收紧上限，再 `fillMaxWidth` 填满这个上限 —— 于是窄窗口下仍是
 * 整幅宽度、宽窗口下才收窄。（反过来写 `fillMaxWidth().widthIn(max)` 会因为前一个修饰符
 * 已经把宽度固定成父级宽度而失效。）
 *
 * 收窄后多出来的空间由**父级的对齐方式**决定去哪（居中 / 靠一侧），本修饰符不预设。
 */
fun Modifier.contentWidth(maxWidth: Dp = ContentWidth.Prose): Modifier =
  widthIn(max = maxWidth).fillMaxWidth()
