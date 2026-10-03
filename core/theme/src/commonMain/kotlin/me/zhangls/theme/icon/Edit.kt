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


val Icons.Rounded.Edit: ImageVector
  get() {
    if (_Edit != null) {
      return _Edit!!
    }
    _Edit = ImageVector.Builder(
      name = "Rounded.Edit",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(200f, 760f)
        horizontalLineToRelative(57f)
        lineToRelative(391f, -391f)
        lineToRelative(-57f, -57f)
        lineToRelative(-391f, 391f)
        verticalLineToRelative(57f)
        close()
        moveToRelative(-40f, 80f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(120f, 800f)
        verticalLineToRelative(-97f)
        quadToRelative(0f, -16f, 6f, -30.5f)
        reflectiveQuadToRelative(17f, -25.5f)
        lineToRelative(505f, -504f)
        quadToRelative(12f, -11f, 26.5f, -17f)
        reflectiveQuadToRelative(30.5f, -6f)
        quadToRelative(16f, 0f, 31f, 6f)
        reflectiveQuadToRelative(26f, 18f)
        lineToRelative(55f, 56f)
        quadToRelative(12f, 11f, 17.5f, 26f)
        reflectiveQuadToRelative(5.5f, 30f)
        quadToRelative(0f, 16f, -5.5f, 30.5f)
        reflectiveQuadTo(817f, 313f)
        lineTo(313f, 817f)
        quadToRelative(-11f, 11f, -25.5f, 17f)
        reflectiveQuadToRelative(-30.5f, 6f)
        horizontalLineToRelative(-97f)
        close()
        moveToRelative(600f, -584f)
        lineToRelative(-56f, -56f)
        lineToRelative(56f, 56f)
        close()
        moveToRelative(-141f, 85f)
        lineToRelative(-28f, -29f)
        lineToRelative(57f, 57f)
        lineToRelative(-29f, -28f)
        close()
      }
    }.build()

    return _Edit!!
  }

@Suppress("ObjectPropertyName")
private var _Edit: ImageVector? = null

@Preview(name = "Edit", showBackground = true)
@Composable
private fun EditPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
  }
}
