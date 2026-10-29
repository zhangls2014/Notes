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

val Icons.Rounded.ViewAgenda: ImageVector
  get() {
    cachedViewAgenda?.let { return it }
    val image = ImageVector.Builder(
      name = "Rounded.ViewAgenda",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(200f, 440f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 360f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 120f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 200f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 440f)
        horizontalLineTo(200f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-160f)
        horizontalLineTo(200f)
        verticalLineToRelative(160f)
        close()
        moveToRelative(0f, 480f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 760f)
        verticalLineToRelative(-160f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 520f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 600f)
        verticalLineToRelative(160f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 840f)
        horizontalLineTo(200f)
        close()
        moveToRelative(0f, -80f)
        horizontalLineToRelative(560f)
        verticalLineToRelative(-160f)
        horizontalLineTo(200f)
        verticalLineToRelative(160f)
        close()
        moveToRelative(0f, -560f)
        verticalLineToRelative(160f)
        verticalLineToRelative(-160f)
        close()
        moveToRelative(0f, 400f)
        verticalLineToRelative(160f)
        verticalLineToRelative(-160f)
        close()
      }
    }.build()

    cachedViewAgenda = image
    return image
  }

private var cachedViewAgenda: ImageVector? = null

@Preview(name = "ViewAgenda", showBackground = true)
@Composable
private fun ViewAgendaPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.ViewAgenda, contentDescription = null)
  }
}
