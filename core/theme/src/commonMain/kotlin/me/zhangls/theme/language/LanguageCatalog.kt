package me.zhangls.theme.language

import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.data.type.AppLanguage
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Language
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.language_chinese
import notes.core.theme.generated.resources.language_english
import notes.core.theme.generated.resources.language_follow_system
import notes.core.theme.generated.resources.language_label
import org.jetbrains.compose.resources.StringResource

/**
 * 单个语言选项：展示文案与实际取值。
 *
 * @author zhangls
 */
data class LanguageOption(
  val label: StringResource,
  val value: AppLanguage,
)

/**
 * 应用语言选项的共享目录。
 *
 * 语言切换在登录页与设置页均需展示，其标题 / 选项 / 图标属于跨 feature 的共享 UI 数据，
 * 因此下沉到 `core:theme`，供各方复用，避免 feature 间为此产生直接依赖。
 *
 * @author zhangls
 */
object LanguageCatalog {
  val title: StringResource = Res.string.language_label

  val icon: ImageVector = Icons.Rounded.Language

  val options: List<LanguageOption> = listOf(
    LanguageOption(Res.string.language_follow_system, AppLanguage.FOLLOW_SYSTEM),
    LanguageOption(Res.string.language_english, AppLanguage.ENGLISH),
    LanguageOption(Res.string.language_chinese, AppLanguage.CHINESE),
  )

  fun summary(language: AppLanguage): StringResource = when (language) {
    AppLanguage.FOLLOW_SYSTEM -> Res.string.language_follow_system
    AppLanguage.ENGLISH -> Res.string.language_english
    AppLanguage.CHINESE -> Res.string.language_chinese
  }
}
