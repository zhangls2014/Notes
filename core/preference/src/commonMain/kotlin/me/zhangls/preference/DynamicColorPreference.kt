package me.zhangls.preference

import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Palette
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.settings_label_dynamic_color
import notes.core.preference.generated.resources.settings_msg_dynamic_color_off
import notes.core.preference.generated.resources.settings_msg_dynamic_color_on

/**
 * 动态主题色的设置项元数据（仅 Android 支持动态取色，是否展示由消费方决定）。
 *
 * @author zhangls
 */
object DynamicColorPreference {
  val spec: PreferenceSpec.Toggle = PreferenceSpec.Toggle(
    key = "dynamicColor",
    title = Res.string.settings_label_dynamic_color,
    icon = Icons.Rounded.Palette,
    summary = { enabled ->
      if (enabled) {
        Res.string.settings_msg_dynamic_color_on
      } else {
        Res.string.settings_msg_dynamic_color_off
      }
    },
  )
}
