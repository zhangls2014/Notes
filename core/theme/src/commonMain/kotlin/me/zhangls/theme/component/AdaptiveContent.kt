package me.zhangls.theme.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.contentWidth

/**
 * 给内容加一个宽度上限，超出的空间在两侧留白（内容居中）。
 *
 * 为什么需要它：窗口可以宽到 1600dp（Android 17 起大屏设备不再允许应用锁定方向与尺寸），
 * 而"一行设置项""一段正文"的可用宽度并不随之增长 —— 行宽超出可读范围后，阅读与扫视都变差。
 *
 * 上限取值集中在 [ContentWidth]，本组件只是把它套上"居中 + 填满上限"这层渲染。
 *
 * 反过来说：真正的多形态适配（分栏、导航套件形态、窗格宽度）不该在这里做 ——
 * 那是 [me.zhangls.theme.layout.LocalWindowAdaptiveInfo] 与场景策略的职责。
 * 这个组件只回答"一行多长算合适"。
 *
 * @param maxWidth 内容宽度上限，默认 [ContentWidth.Prose]
 */
@Composable
fun AdaptiveContent(
  modifier: Modifier = Modifier,
  maxWidth: Dp = ContentWidth.Prose,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Box(modifier = Modifier.contentWidth(maxWidth)) {
      content()
    }
  }
}
