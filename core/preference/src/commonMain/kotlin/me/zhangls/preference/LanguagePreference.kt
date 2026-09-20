package me.zhangls.preference

import me.zhangls.model.AppLanguage
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Language
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.language_chinese
import notes.core.preference.generated.resources.language_english
import notes.core.preference.generated.resources.language_follow_system
import notes.core.preference.generated.resources.language_label

/**
 * 应用语言的设置项元数据。
 *
 * 语言切换在登录页（右上角图标按钮 + 下拉）与设置页（列表项）以两种完全不同的形态出现，
 * 但标题 / 图标 / 选项完全一致，故登记在此处供双方复用，避免各自维护一份文案。
 *
 * @author zhangls
 */
object LanguagePreference {
  val spec: PreferenceSpec.Select<AppLanguage> = PreferenceSpec.Select(
    key = "appLanguage",
    title = Res.string.language_label,
    icon = Icons.Rounded.Language,
    options = listOf(
      PreferenceOption(Res.string.language_follow_system, AppLanguage.FOLLOW_SYSTEM),
      PreferenceOption(Res.string.language_english, AppLanguage.ENGLISH),
      PreferenceOption(Res.string.language_chinese, AppLanguage.CHINESE),
    ),
  )
}
