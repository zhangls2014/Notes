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

val Icons.Rounded.ViewColumn: ImageVector
  get() {
    cachedViewColumn?.let { return it }
    val image = ImageVector.Builder(
      name = "Rounded.ViewColumn",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(121f, 680f)
        verticalLineToRelative(-400f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(201f, 200f)
        horizontalLineToRelative(559f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 280f)
        verticalLineToRelative(400f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 760f)
        horizontalLineTo(201f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(121f, 680f)
        close()
        moveToRelative(79f, 0f)
        horizontalLineToRelative(133f)
        verticalLineToRelative(-400f)
        horizontalLineTo(200f)
        verticalLineToRelative(400f)
        close()
        moveToRelative(213f, 0f)
        horizontalLineToRelative(133f)
        verticalLineToRelative(-400f)
        horizontalLineTo(413f)
        verticalLineToRelative(400f)
        close()
        moveToRelative(213f, 0f)
        horizontalLineToRelative(133f)
        verticalLineToRelative(-400f)
        horizontalLineTo(626f)
        verticalLineToRelative(400f)
        close()
      }
    }.build()

    cachedViewColumn = image
    return image
  }

private var cachedViewColumn: ImageVector? = null

@Preview(name = "ViewColumn", showBackground = true)
@Composable
private fun ViewColumnPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.ViewColumn, contentDescription = null)
  }
}
