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


val Icons.Rounded.ExitToApp: ImageVector
  get() {
    if (_ExitToApp != null) {
      return _ExitToApp!!
    }
    _ExitToApp = ImageVector.Builder(
      name = "Rounded.ExitToApp",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(200f, 840f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 760f)
        verticalLineToRelative(-120f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 600f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(200f, 640f)
        verticalLineToRelative(120f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-560f)
        horizontalLineTo(200f)
        verticalLineToRelative(120f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(160f, 360f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 320f)
        verticalLineToRelative(-120f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 120f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 200f)
        verticalLineToRelative(560f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 840f)
        horizontalLineTo(200f)
        close()
        moveToRelative(266f, -320f)
        horizontalLineTo(160f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 480f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(160f, 440f)
        horizontalLineToRelative(306f)
        lineToRelative(-74f, -74f)
        quadToRelative(-12f, -12f, -11.5f, -28f)
        reflectiveQuadToRelative(11.5f, -28f)
        quadToRelative(12f, -12f, 28.5f, -12.5f)
        reflectiveQuadTo(449f, 309f)
        lineToRelative(143f, 143f)
        quadToRelative(6f, 6f, 8.5f, 13f)
        reflectiveQuadToRelative(2.5f, 15f)
        quadToRelative(0f, 8f, -2.5f, 15f)
        reflectiveQuadToRelative(-8.5f, 13f)
        lineTo(449f, 651f)
        quadToRelative(-12f, 12f, -28.5f, 11.5f)
        reflectiveQuadTo(392f, 650f)
        quadToRelative(-11f, -12f, -11.5f, -28f)
        reflectiveQuadToRelative(11.5f, -28f)
        lineToRelative(74f, -74f)
        close()
      }
    }.build()

    return _ExitToApp!!
  }

@Suppress("ObjectPropertyName")
private var _ExitToApp: ImageVector? = null

@Preview(name = "ExitToApp", showBackground = true)
@Composable
private fun ExitToAppPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.ExitToApp, contentDescription = null)
  }
}
