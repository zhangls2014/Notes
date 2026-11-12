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

val Icons.Rounded.Mail: ImageVector
  get() {
    cachedMail?.let { return it }
    val image = ImageVector.Builder(
      name = "Rounded.Mail",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(160f, 800f)
        quadToRelative(-33f, 0f, -56.5f, -23.5f)
        reflectiveQuadTo(80f, 720f)
        verticalLineToRelative(-480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(160f, 160f)
        horizontalLineToRelative(640f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(880f, 240f)
        verticalLineToRelative(480f)
        quadToRelative(0f, 33f, -23.5f, 56.5f)
        reflectiveQuadTo(800f, 800f)
        horizontalLineTo(160f)
        close()
        moveToRelative(640f, -480f)
        lineTo(501f, 507f)
        quadToRelative(-5f, 3f, -10.5f, 4.5f)
        reflectiveQuadTo(480f, 513f)
        quadToRelative(-5f, 0f, -10.5f, -1.5f)
        reflectiveQuadTo(459f, 507f)
        lineTo(160f, 320f)
        verticalLineToRelative(400f)
        horizontalLineToRelative(640f)
        verticalLineToRelative(-400f)
        close()
        moveTo(480f, 440f)
        lineToRelative(320f, -200f)
        horizontalLineTo(160f)
        lineToRelative(320f, 200f)
        close()
        moveTo(160f, 320f)
        verticalLineToRelative(10f)
        verticalLineToRelative(-59f)
        verticalLineToRelative(1f)
        verticalLineToRelative(-32f)
        verticalLineToRelative(32f)
        verticalLineToRelative(-0.5f)
        verticalLineToRelative(58.5f)
        verticalLineToRelative(-10f)
        verticalLineToRelative(400f)
        verticalLineToRelative(-400f)
        close()
      }
    }.build()

    cachedMail = image
    return image
  }

private var cachedMail: ImageVector? = null

@Preview(name = "Mail", showBackground = true)
@Composable
private fun MailPreview() {
  Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
    Icon(imageVector = Icons.Rounded.Mail, contentDescription = null)
  }
}
