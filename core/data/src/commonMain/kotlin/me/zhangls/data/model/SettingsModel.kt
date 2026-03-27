package me.zhangls.data.model

import kotlinx.serialization.Serializable
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig


@Serializable
data class SettingsModel(
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT,
  val fontSize: FontSizeConfig = FontSizeConfig.STANDARD,
  val appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM
)
