package me.zhangls.entry

import me.zhangls.data.type.AppLanguage
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

object LanguageManager {
  private const val LANGUAGE_KEY = "appLanguage"
  private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
  const val LANGUAGE_DID_CHANGE_NOTIFICATION = "me.zhangls.entry.languageDidChange"

  fun updateLanguage(language: AppLanguage) {
    val userDefaults = NSUserDefaults.standardUserDefaults
    if (language.name == userDefaults.objectForKey(LANGUAGE_KEY)) return
    userDefaults.setObject(language.name, forKey = LANGUAGE_KEY)

    val changed = applyAppleLanguages(language, userDefaults)
    userDefaults.synchronize()
    if (changed) {
      NSNotificationCenter.defaultCenter.postNotificationName(LANGUAGE_DID_CHANGE_NOTIFICATION, null)
    }
  }

  private fun applyAppleLanguages(language: AppLanguage, userDefaults: NSUserDefaults): Boolean {
    val targetLanguageTag = when (language) {
      AppLanguage.FOLLOW_SYSTEM -> null
      AppLanguage.ENGLISH -> "en-CN"
      AppLanguage.CHINESE -> "zh-Hans-CN"
    }

    val currentTags = userDefaults.stringArrayForKey(APPLE_LANGUAGES_KEY)?.mapNotNull { it as? String }.orEmpty()

    return if (targetLanguageTag == null) {
      if (currentTags.isEmpty()) {
        false
      } else {
        // 跟随系统，则删除 App 语言优先级设置
        userDefaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        true
      }
    } else {
      if (currentTags.size == 1 && currentTags.first().startsWith(targetLanguageTag)) {
        false
      } else {
        userDefaults.setObject(listOf(targetLanguageTag), forKey = APPLE_LANGUAGES_KEY)
        true
      }
    }
  }
}
