package me.zhangls.theme.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Icons.Rounded.MoreVert: ImageVector
  get() {
    cachedMoreVert?.let { return it }
    val image = ImageVector.Builder(
      name = "Rounded.MoreVert",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(480f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadToRelative(-23.5f, -56.5f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadToRelative(56.5f, -23.5f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadToRelative(23.5f, 56.5f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadToRelative(-56.5f, 23.5f)
        close()
        moveToRelative(0f, -240f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadToRelative(-23.5f, -56.5f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadToRelative(56.5f, -23.5f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadToRelative(23.5f, 56.5f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadToRelative(-56.5f, 23.5f)
        close()
        moveToRelative(0f, -240f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadToRelative(-23.5f, -56.5f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadToRelative(56.5f, -23.5f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadToRelative(23.5f, 56.5f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadToRelative(-56.5f, 23.5f)
        close()
      }
    }.build()

    cachedMoreVert = image
    return image
  }

private var cachedMoreVert: ImageVector? = null

@Preview(name = "MoreVert", showBackground = true)
@Composable
private fun MoreVertPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
  }
}
