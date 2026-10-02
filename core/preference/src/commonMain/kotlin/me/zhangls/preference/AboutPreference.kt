package me.zhangls.preference

import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Info
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.settings_label_about

/** Metadata only; settings owns the action and position in its list. */
object AboutPreference {
  val spec = PreferenceSpec.Action(
    key = "about",
    title = Res.string.settings_label_about,
    icon = Icons.Rounded.Info,
  )
}
