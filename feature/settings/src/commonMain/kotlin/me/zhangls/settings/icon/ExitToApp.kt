package me.zhangls.settings.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.zhangls.theme.icon.Icons

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
      viewportHeight = 960f
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
        lineTo(200f, 200f)
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
        lineTo(200f, 840f)
        close()
        moveTo(466f, 520f)
        lineTo(160f, 520f)
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
