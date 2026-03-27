package me.zhangls.login.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppLogo: ImageVector
  get() {
    if (_AppLogo != null) {
      return _AppLogo!!
    }
    _AppLogo = ImageVector.Builder(
      name = "AppLogo",
      defaultWidth = 108.dp,
      defaultHeight = 108.dp,
      viewportWidth = 108f,
      viewportHeight = 108f
    ).apply {
      path(fill = SolidColor(Color(0xFF0F2A44))) {
        moveTo(24f, 0f)
        lineTo(84f, 0f)
        arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 108f, 24f)
        lineTo(108f, 84f)
        arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 84f, 108f)
        lineTo(24f, 108f)
        arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 84f)
        lineTo(0f, 24f)
        arcTo(24f, 24f, 0f, isMoreThanHalf = false, isPositiveArc = true, 24f, 0f)
        close()
      }
      path(fill = SolidColor(Color(0xFF1E4F7A))) {
        moveTo(28f, 26f)
        horizontalLineTo(76f)
        quadTo(82f, 26f, 82f, 32f)
        verticalLineTo(72f)
        horizontalLineTo(68f)
        verticalLineTo(40f)
        horizontalLineTo(28f)
        close()
      }
      path(fill = SolidColor(Color(0xFF3A7DBA))) {
        moveTo(28f, 72f)
        lineTo(42f, 72f)
        lineTo(28f, 86f)
        close()
      }
      path(fill = SolidColor(Color(0xFFFFD54A))) {
        moveTo(54f, 30f)
        lineTo(61f, 48f)
        lineTo(80f, 48f)
        lineTo(65f, 60f)
        lineTo(71f, 78f)
        lineTo(54f, 67f)
        lineTo(37f, 78f)
        lineTo(43f, 60f)
        lineTo(28f, 48f)
        lineTo(47f, 48f)
        close()
      }
    }.build()

    return _AppLogo!!
  }

@Suppress("ObjectPropertyName")
private var _AppLogo: ImageVector? = null
