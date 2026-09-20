package me.zhangls.email.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.theme.component.TooltipIconButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


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
  val text: StringResource,
  val onAction: () -> Unit = {},
)

@Composable
fun ActionItem(item: ActionItem, onClick: (ActionItem) -> Unit) {
  TooltipIconButton(
    icon = item.icon,
    label = stringResource(item.text),
    onClick = { onClick(item) },
  )
}
