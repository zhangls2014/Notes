package me.zhangls.preference

import me.zhangls.theme.icon.AccountCircle
import me.zhangls.theme.icon.Icons
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.settings_label_profile

/** 个人信息入口的静态元数据；用户数据和导航行为由 Feature 提供。 */
object ProfilePreference {
  val spec = PreferenceSpec.Action(
    key = "profile",
    title = Res.string.settings_label_profile,
    icon = Icons.Rounded.AccountCircle,
  )
}
