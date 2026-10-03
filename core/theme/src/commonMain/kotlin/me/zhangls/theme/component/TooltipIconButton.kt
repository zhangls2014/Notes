package me.zhangls.theme.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 带长按提示的图标按钮。
 *
 * "图标按钮 + 提示气泡"是本项目里反复出现的形态：登录页表单区域的深色模式 / 语言入口、
 * 邮件多选操作栏、搜索栏的返回键 —— 同一段 `TooltipBox { IconButton { Icon } }` 原先
 * 在三处各写了一份，提示位置靠三处硬编码保持一致。下沉到这里后，形态与提示样式只有一个来源。
 *
 * [label] 同时用作提示文本与 `contentDescription`：三者本就该是同一句话，分开传会出现
 * 「看得见提示、读屏读不到」或反之的不一致。
 *
 * @param icon 按钮图标
 * @param label 按钮名称（同时作为无障碍描述）
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param tooltipPosition 提示相对按钮的位置。靠近页面顶部的按钮用 [TooltipAnchorPosition.Below]，
 *   否则提示会被屏幕边缘裁掉；默认 [TooltipAnchorPosition.Above]。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TooltipIconButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tooltipPosition: TooltipAnchorPosition = TooltipAnchorPosition.Above,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(tooltipPosition),
    state = rememberTooltipState(),
    modifier = modifier,
    tooltip = {
      PlainTooltip {
        Text(text = label)
      }
    },
  ) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
      Icon(imageVector = icon, contentDescription = label)
    }
  }
}
