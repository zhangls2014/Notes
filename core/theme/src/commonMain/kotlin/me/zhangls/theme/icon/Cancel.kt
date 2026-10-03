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


val Icons.Rounded.Cancel: ImageVector
  get() {
    if (_Cancel != null) {
      return _Cancel!!
    }
    _Cancel = ImageVector.Builder(
      name = "Rounded.Cancel",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(480f, 536f)
        lineToRelative(116f, 116f)
        quadToRelative(11f, 11f, 28f, 11f)
        reflectiveQuadToRelative(28f, -11f)
        quadToRelative(11f, -11f, 11f, -28f)
        reflectiveQuadToRelative(-11f, -28f)
        lineTo(536f, 480f)
        lineToRelative(116f, -116f)
        quadToRelative(11f, -11f, 11f, -28f)
        reflectiveQuadToRelative(-11f, -28f)
        quadToRelative(-11f, -11f, -28f, -11f)
        reflectiveQuadToRelative(-28f, 11f)
        lineTo(480f, 424f)
        lineTo(364f, 308f)
        quadToRelative(-11f, -11f, -28f, -11f)
        reflectiveQuadToRelative(-28f, 11f)
        quadToRelative(-11f, 11f, -11f, 28f)
        reflectiveQuadToRelative(11f, 28f)
        lineToRelative(116f, 116f)
        lineToRelative(-116f, 116f)
        quadToRelative(-11f, 11f, -11f, 28f)
        reflectiveQuadToRelative(11f, 28f)
        quadToRelative(11f, 11f, 28f, 11f)
        reflectiveQuadToRelative(28f, -11f)
        lineToRelative(116f, -116f)
        close()
        moveToRelative(0f, 344f)
        quadToRelative(-83f, 0f, -156f, -31.5f)
        reflectiveQuadTo(197f, 763f)
        quadToRelative(-54f, -54f, -85.5f, -127f)
        reflectiveQuadTo(80f, 480f)
        quadToRelative(0f, -83f, 31.5f, -156f)
        reflectiveQuadTo(197f, 197f)
        quadToRelative(54f, -54f, 127f, -85.5f)
        reflectiveQuadTo(480f, 80f)
        quadToRelative(83f, 0f, 156f, 31.5f)
        reflectiveQuadTo(763f, 197f)
        quadToRelative(54f, 54f, 85.5f, 127f)
        reflectiveQuadTo(880f, 480f)
        quadToRelative(0f, 83f, -31.5f, 156f)
        reflectiveQuadTo(763f, 763f)
        quadToRelative(-54f, 54f, -127f, 85.5f)
        reflectiveQuadTo(480f, 880f)
        close()
        moveToRelative(0f, -80f)
        quadToRelative(134f, 0f, 227f, -93f)
        reflectiveQuadToRelative(93f, -227f)
        quadToRelative(0f, -134f, -93f, -227f)
        reflectiveQuadToRelative(-227f, -93f)
        quadToRelative(-134f, 0f, -227f, 93f)
        reflectiveQuadToRelative(-93f, 227f)
        quadToRelative(0f, 134f, 93f, 227f)
        reflectiveQuadToRelative(227f, 93f)
        close()
        moveToRelative(0f, -320f)
        close()
      }
    }.build()

    return _Cancel!!
  }

@Suppress("ObjectPropertyName")
private var _Cancel: ImageVector? = null

@Preview(name = "Cancel", showBackground = true)
@Composable
private fun CancelPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.Cancel, contentDescription = null)
  }
}
