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


val Icons.Rounded.ArrowBackIosNew: ImageVector
  get() {
    if (_ArrowBackIosNew != null) {
      return _ArrowBackIosNew!!
    }
    _ArrowBackIosNew = ImageVector.Builder(
      name = "Rounded.ArrowBackIosNew",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(382f, 480f)
        lineToRelative(294f, 294f)
        quadToRelative(15f, 15f, 14.5f, 35f)
        reflectiveQuadTo(675f, 844f)
        quadToRelative(-15f, 15f, -35f, 15f)
        reflectiveQuadToRelative(-35f, -15f)
        lineTo(297f, 537f)
        quadToRelative(-12f, -12f, -18f, -27f)
        reflectiveQuadToRelative(-6f, -30f)
        quadToRelative(0f, -15f, 6f, -30f)
        reflectiveQuadToRelative(18f, -27f)
        lineToRelative(308f, -308f)
        quadToRelative(15f, -15f, 35.5f, -14.5f)
        reflectiveQuadTo(676f, 116f)
        quadToRelative(15f, 15f, 15f, 35f)
        reflectiveQuadToRelative(-15f, 35f)
        lineTo(382f, 480f)
        close()
      }
    }.build()

    return _ArrowBackIosNew!!
  }

@Suppress("ObjectPropertyName")
private var _ArrowBackIosNew: ImageVector? = null

@Preview(name = "ArrowBackIosNew", showBackground = true)
@Composable
private fun ArrowBackIosNewPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.ArrowBackIosNew, contentDescription = null)
  }
}
