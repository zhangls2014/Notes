package me.zhangls.framework.toast

internal data class ToastInsets(
  val top: Double = 0.0,
  val left: Double = 0.0,
  val bottom: Double = 0.0,
  val right: Double = 0.0,
)

internal data class ToastFrame(
  val x: Double,
  val y: Double,
  val width: Double,
  val height: Double,
)

/** Layout in the host root view's coordinates, including small resizable windows. */
internal fun toastFrame(
  windowWidth: Double,
  windowHeight: Double,
  insets: ToastInsets,
  textWidth: Double,
  textHeight: Double,
): ToastFrame {
  val safeWidth = (windowWidth - insets.left - insets.right).coerceAtLeast(0.0)
  val safeHeight = (windowHeight - insets.top - insets.bottom).coerceAtLeast(0.0)
  val width = (textWidth + 40.0).coerceIn(0.0, (safeWidth - 60.0).coerceAtLeast(0.0))
  val height = (textHeight + 28.0).coerceIn(0.0, safeHeight)
  val bottomMargin = maxOf(100.0, insets.bottom + 32.0)
  return ToastFrame(
    x = insets.left + (safeWidth - width) / 2.0,
    y = maxOf(insets.top, windowHeight - bottomMargin - height),
    width = width,
    height = height,
  )
}
