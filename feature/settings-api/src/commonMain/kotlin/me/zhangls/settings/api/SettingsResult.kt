package me.zhangls.settings.api

import androidx.compose.runtime.staticCompositionLocalOf
import me.zhangls.framework.mvi.MviEffect

/** 需要宿主参与导航的设置页结果；设置值通过 Repository 流广播。 */
sealed interface SettingsResult : MviEffect {
  data object OpenProfile : SettingsResult
  data object OpenAbout : SettingsResult
  data class ChangeBiometricUnlock(val enabled: Boolean) : SettingsResult
  data object ManagePasskeys : SettingsResult
  data object Logout : SettingsResult
}

val LocalPasskeyManagementAvailable = staticCompositionLocalOf { false }
