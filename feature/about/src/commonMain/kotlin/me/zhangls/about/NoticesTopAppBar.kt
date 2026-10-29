package me.zhangls.about

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Close
import me.zhangls.theme.icon.Icons
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_close_detail
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NoticesTopAppBar(title: String, onBack: () -> Unit, isDetail: Boolean) {
  val scene = LocalListDetailSceneScope.current
  val listVisible = scene != null && scene.scaffoldTransitionScope.scaffoldStateTransition.targetState[
    ListDetailPaneScaffoldRole.List,
  ] != PaneAdaptedValue.Hidden
  val showClose = isDetail && listVisible
  CenteredTopAppBar(
    title = title,
    navigate = onBack.takeUnless { showClose },
    actions = {
      if (showClose) {
        IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
          Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(Res.string.about_close_detail))
        }
      }
    },
  )
}
