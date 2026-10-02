package me.zhangls.theme.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.theme_app_icon
import notes.core.theme.generated.resources.theme_app_name
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Shared brand assets; generated resource accessors remain private to this module. */
@Composable
fun appName(): String = stringResource(Res.string.theme_app_name)

@Composable
fun AppIcon(modifier: Modifier = Modifier) {
  Image(painterResource(Res.drawable.theme_app_icon), contentDescription = null, modifier = modifier)
}
