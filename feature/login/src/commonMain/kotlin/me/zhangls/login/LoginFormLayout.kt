package me.zhangls.login

internal data class LoginFormPlacement(val height: Int, val bodyTop: Int)

/** Center the form body when it fits; grow the scroll content instead of clipping it otherwise. */
internal fun loginFormPlacement(
  viewportHeight: Int,
  settingsHeight: Int,
  bodyHeight: Int,
): LoginFormPlacement {
  val height = maxOf(viewportHeight, settingsHeight + bodyHeight)
  return LoginFormPlacement(
    height = height,
    bodyTop = maxOf(settingsHeight, (height - bodyHeight) / 2),
  )
}
