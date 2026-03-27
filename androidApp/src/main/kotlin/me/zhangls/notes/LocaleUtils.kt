package me.zhangls.notes

import androidx.core.os.LocaleListCompat
import me.zhangls.data.type.AppLanguage

fun AppLanguage.toLocales(): LocaleListCompat {
  return when (this) {
    AppLanguage.FOLLOW_SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
    AppLanguage.CHINESE -> LocaleListCompat.forLanguageTags("zh-Hans-CN,zh-CN,zh")
  }
}
