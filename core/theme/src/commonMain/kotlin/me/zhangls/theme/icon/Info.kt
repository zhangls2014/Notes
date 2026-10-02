package me.zhangls.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material information symbol. */
val Icons.Rounded.Info: ImageVector get() = infoIcon

private val infoIcon: ImageVector by lazy {
  ImageVector.Builder("Rounded.Info", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
      moveTo(12f, 2f)
      arcToRelative(10f, 10f, 0f, true, false, 0f, 20f)
      arcToRelative(10f, 10f, 0f, true, false, 0f, -20f)
      close()
      moveTo(11f, 7f); lineTo(13f, 7f); lineTo(13f, 9f); lineTo(11f, 9f); close()
      moveTo(11f, 11f); lineTo(13f, 11f); lineTo(13f, 17f); lineTo(11f, 17f); close()
    }
  }.build()
}
