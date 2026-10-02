package me.zhangls.theme.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.theme_ic_default_avatar
import org.jetbrains.compose.resources.painterResource

/** 共享的用户头像展示；大小、语义和交互由调用方决定。 */
@Composable
fun UserAvatar(path: String?, contentDescription: String?, modifier: Modifier = Modifier) {
  AsyncImage(
    model = path,
    contentDescription = contentDescription,
    modifier = modifier.clip(CircleShape),
    placeholder = painterResource(Res.drawable.theme_ic_default_avatar),
    error = painterResource(Res.drawable.theme_ic_default_avatar),
    contentScale = ContentScale.Crop,
  )
}
