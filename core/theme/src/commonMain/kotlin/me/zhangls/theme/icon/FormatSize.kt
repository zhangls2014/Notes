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


val Icons.Rounded.FormatSize: ImageVector
  get() {
    if (_FormatSize != null) {
      return _FormatSize!!
    }
    _FormatSize = ImageVector.Builder(
      name = "Rounded.FormatSize",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(560f, 280f)
        horizontalLineTo(420f)
        quadToRelative(-25f, 0f, -42.5f, -17.5f)
        reflectiveQuadTo(360f, 220f)
        quadToRelative(0f, -25f, 17.5f, -42.5f)
        reflectiveQuadTo(420f, 160f)
        horizontalLineToRelative(400f)
        quadToRelative(25f, 0f, 42.5f, 17.5f)
        reflectiveQuadTo(880f, 220f)
        quadToRelative(0f, 25f, -17.5f, 42.5f)
        reflectiveQuadTo(820f, 280f)
        horizontalLineTo(680f)
        verticalLineToRelative(460f)
        quadToRelative(0f, 25f, -17.5f, 42.5f)
        reflectiveQuadTo(620f, 800f)
        quadToRelative(-25f, 0f, -42.5f, -17.5f)
        reflectiveQuadTo(560f, 740f)
        verticalLineToRelative(-460f)
        close()
        moveTo(200f, 480f)
        horizontalLineToRelative(-60f)
        quadToRelative(-25f, 0f, -42.5f, -17.5f)
        reflectiveQuadTo(80f, 420f)
        quadToRelative(0f, -25f, 17.5f, -42.5f)
        reflectiveQuadTo(140f, 360f)
        horizontalLineToRelative(240f)
        quadToRelative(25f, 0f, 42.5f, 17.5f)
        reflectiveQuadTo(440f, 420f)
        quadToRelative(0f, 25f, -17.5f, 42.5f)
        reflectiveQuadTo(380f, 480f)
        horizontalLineToRelative(-60f)
        verticalLineToRelative(260f)
        quadToRelative(0f, 25f, -17.5f, 42.5f)
        reflectiveQuadTo(260f, 800f)
        quadToRelative(-25f, 0f, -42.5f, -17.5f)
        reflectiveQuadTo(200f, 740f)
        verticalLineToRelative(-260f)
        close()
      }
    }.build()

    return _FormatSize!!
  }

@Suppress("ObjectPropertyName")
private var _FormatSize: ImageVector? = null

@Preview(name = "FormatSize", showBackground = true)
@Composable
private fun FormatSizePreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.FormatSize, contentDescription = null)
  }
}
