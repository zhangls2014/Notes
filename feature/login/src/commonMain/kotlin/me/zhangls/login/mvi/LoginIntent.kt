package me.zhangls.login.mvi

import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface LoginIntent : MviIntent {
  object Login : LoginIntent
  data class UpdateAccount(val account: String) : LoginIntent
  object ClearAccount : LoginIntent
  data class UpdatePassword(val password: String) : LoginIntent
  data class UpdatePasswordVisible(val visible: Boolean) : LoginIntent
  data class UpdateLanguage(val language: AppLanguage) : LoginIntent
  data class UpdateDarkTheme(val config: DarkThemeConfig) : LoginIntent
}
