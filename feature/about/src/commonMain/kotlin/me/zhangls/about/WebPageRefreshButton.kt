package me.zhangls.about

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.Composable
import me.zhangls.theme.component.TooltipIconButton
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Refresh
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_refresh_page
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebPageRefreshButton(onClick: () -> Unit) {
  TooltipIconButton(
    icon = Icons.Rounded.Refresh,
    label = stringResource(Res.string.about_refresh_page),
    onClick = onClick,
    tooltipPosition = TooltipAnchorPosition.Below,
  )
}
