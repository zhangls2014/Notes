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

val Icons.Rounded.Storage: ImageVector
  get() {
    cachedStorage?.let { return it }
    val image = ImageVector.Builder(
      name = "Rounded.Storage",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(200f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 720f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 640f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 720f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 800f)
        horizontalLineTo(200f)
        close()
        moveToRelative(0f, -480f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 240f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 160f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 240f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 320f)
        horizontalLineTo(200f)
        close()
        moveToRelative(0f, 240f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(120f, 480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(200f, 400f)
        horizontalLineToRelative(560f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(840f, 480f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(760f, 560f)
        horizontalLineTo(200f)
        close()
        moveToRelative(40f, -280f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(280f, 240f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(240f, 200f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(200f, 240f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(240f, 280f)
        close()
        moveToRelative(0f, 240f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(280f, 480f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(240f, 440f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(200f, 480f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(240f, 520f)
        close()
        moveToRelative(0f, 240f)
        quadToRelative(17f, 0f, 28.5f, -11.5f)
        reflectiveQuadTo(280f, 720f)
        quadToRelative(0f, -17f, -11.5f, -28.5f)
        reflectiveQuadTo(240f, 680f)
        quadToRelative(-17f, 0f, -28.5f, 11.5f)
        reflectiveQuadTo(200f, 720f)
        quadToRelative(0f, 17f, 11.5f, 28.5f)
        reflectiveQuadTo(240f, 760f)
        close()
      }
    }.build()

    cachedStorage = image
    return image
  }

private var cachedStorage: ImageVector? = null

@Preview(name = "Storage", showBackground = true)
@Composable
private fun StoragePreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.Storage, contentDescription = null)
  }
}
