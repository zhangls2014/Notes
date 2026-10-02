package me.zhangls.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.Refresh: ImageVector get() = refreshIcon

private val refreshIcon: ImageVector by lazy {
  ImageVector.Builder("Rounded.Refresh", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(17.65f, 6.35f)
      arcTo(8f, 8f, 0f, false, false, 4f, 12f)
      arcTo(8f, 8f, 0f, false, false, 19.75f, 14f)
      lineTo(17.68f, 14f)
      arcTo(6f, 6f, 0f, false, true, 6f, 12f)
      arcTo(6f, 6f, 0f, false, true, 16.24f, 7.76f)
      lineTo(13f, 11f); lineTo(20f, 11f); lineTo(20f, 4f); close()
    }
  }.build()
}
