package me.zhangls.main.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.zhangls.main.icon.Check
import me.zhangls.theme.icon.Icons
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.painterResource


@Composable
fun ProfileImage(drawableKey: String, description: String) {
  val drawableResource = Res.allDrawableResources.getValue(drawableKey)

  Image(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape),
    painter = painterResource(drawableResource),
    contentScale = ContentScale.Crop,
    contentDescription = description,
  )
}

@Composable
fun SelectedProfileImage() {
  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.primary),
  ) {
    Icon(
      imageVector = Icons.Rounded.Check,
      contentDescription = null,
      modifier = Modifier
        .size(24.dp)
        .align(Alignment.Center),
      tint = MaterialTheme.colorScheme.onPrimary,
    )
  }
}
