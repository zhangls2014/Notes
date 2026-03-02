package me.zhangls.data.model

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class SettingsModel(
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT
)
