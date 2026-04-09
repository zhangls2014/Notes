package me.zhangls.email.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.zhangls.theme.icon.Icons

val Icons.Rounded.Clear: ImageVector
  get() {
    if (_Clear != null) {
      return _Clear!!
    }
    _Clear = ImageVector.Builder(
      name = "Rounded.Clear",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(480f, 536f)
        lineTo(284f, 732f)
        quadToRelative(-11f, 11f, -28f, 11f)
        reflectiveQuadToRelative(-28f, -11f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        lineToRelative(196f, -196f)
        lineToRelative(-196f, -196f)
        quadToRelative(-11f, -11f, -11f, -28f)
        reflectiveQuadToRelative(11f, -28f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        lineToRelative(196f, 196f)
        lineToRelative(196f, -196f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        quadToRelative(11f, 11f, 11f, 28f)
        reflectiveQuadToRelative(-11f, 28f)
        lineTo(536f, 480f)
        lineToRelative(196f, 196f)
        quadToRelative(11f, 11f, 11f, 28f)
        reflectiveQuadToRelative(-11f, 28f)
        quadToRelative(-11f, 11f, -28f, 11f)
        reflectiveQuadToRelative(-28f, -11f)
        lineTo(480f, 536f)
        close()
      }
    }.build()

    return _Clear!!
  }

@Suppress("ObjectPropertyName")
private var _Clear: ImageVector? = null
