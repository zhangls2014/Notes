package me.zhangls.main.compose

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import me.zhangls.framework.ext.withDebounce

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmailActionBar(modifier: Modifier = Modifier, items: List<ActionItem>, onClick: (ActionItem) -> Unit) {
  HorizontalFloatingToolbar(
    expanded = true,
    modifier = modifier,
    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    content = {
      items.forEach { ActionItem(it, onClick) }
    },
  )
}

data class ActionItem(
  val icon: ImageVector,
  val text: Int,
)

@Composable
fun ActionItem(item: ActionItem, onClick: (ActionItem) -> Unit) {
  val actionClick = remember(item, onClick) { { onClick(item) }.withDebounce() }
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(stringResource(item.text)) } },
    state = rememberTooltipState(),
  ) {
    IconButton(onClick = actionClick) {
      Icon(imageVector = item.icon, contentDescription = stringResource(item.text))
    }
  }
}
