package me.zhangls.data.model

import kotlinx.serialization.Serializable
import me.zhangls.data.type.DarkThemeConfig

/**
 * @author zhangls
 */
@Serializable
data class SettingsModel(
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT
)
