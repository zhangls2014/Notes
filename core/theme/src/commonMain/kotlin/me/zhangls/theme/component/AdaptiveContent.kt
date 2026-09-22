package me.zhangls.theme.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 列表 / 设置行类内容的默认宽度上限。 */
val DefaultContentMaxWidth = 840.dp

/**
 * 给内容加一个宽度上限，超出的空间在两侧留白（内容居中）。
 *
 * 为什么需要它：窗口可以宽到 1600dp（Android 17 起大屏设备不再允许应用锁定方向与尺寸），
 * 而"一行设置项""一段正文"的可用宽度并不随之增长 —— 行宽超出可读范围后，阅读与扫视都变差。
 * 上限与窗口尺寸无关，因此这里是**一个可读性常量**，不是多形态适配的分支。
 *
 * 反过来说：真正的多形态适配（分栏、导航套件形态、窗格宽度）不该在这里做 ——
 * 那是 `LocalWindowAdaptiveInfo` 与场景策略的职责。这个组件只回答"一行多长算合适"。
 *
 * @param maxWidth 内容宽度上限，默认 [DefaultContentMaxWidth]
 */
@Composable
fun AdaptiveContent(
  modifier: Modifier = Modifier,
  maxWidth: Dp = DefaultContentMaxWidth,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    // widthIn 在前、fillMaxWidth 在后：先给子节点收紧上限，再让它填满这个上限，
    // 于是窄窗口下仍是整幅宽度、宽窗口下才收窄。
    Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()) {
      content()
    }
  }
}
