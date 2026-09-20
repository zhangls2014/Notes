package me.zhangls.preference

import me.zhangls.theme.ThemeColor
import me.zhangls.theme.icon.ExitToApp
import me.zhangls.theme.icon.Icons
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.settings_label_logout

/**
 * 退出登录的设置项元数据。
 *
 * 只描述"长什么样" —— 点击后弹确认框、确认后清空登录态这一整套行为属于 `feature:settings`，
 * 由它把本元数据与自己的 Intent 绑在一起。这是"元数据不含行为"这条约束的直接体现。
 *
 * @author zhangls
 */
object LogoutPreference {
  val spec: PreferenceSpec.Action = PreferenceSpec.Action(
    key = "logout",
    title = Res.string.settings_label_logout,
    icon = Icons.Rounded.ExitToApp,
    tint = ThemeColor.Error,
  )
}
