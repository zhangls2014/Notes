package me.zhangls.preference

import me.zhangls.model.FontSizeConfig
import me.zhangls.theme.icon.FormatSize
import me.zhangls.theme.icon.Icons
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.settings_label_font_size
import notes.core.preference.generated.resources.settings_label_font_size_large
import notes.core.preference.generated.resources.settings_label_font_size_medium
import notes.core.preference.generated.resources.settings_label_font_size_standard

/**
 * 字体大小的设置项元数据。
 *
 * @author zhangls
 */
object FontSizePreference {
  val spec: PreferenceSpec.Select<FontSizeConfig> = PreferenceSpec.Select(
    key = "fontSize",
    title = Res.string.settings_label_font_size,
    icon = Icons.Rounded.FormatSize,
    options = listOf(
      PreferenceOption(Res.string.settings_label_font_size_standard, FontSizeConfig.STANDARD),
      PreferenceOption(Res.string.settings_label_font_size_medium, FontSizeConfig.MEDIUM),
      PreferenceOption(Res.string.settings_label_font_size_large, FontSizeConfig.LARGE),
    ),
  )
}
