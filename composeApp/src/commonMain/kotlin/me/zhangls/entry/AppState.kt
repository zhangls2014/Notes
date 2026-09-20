package me.zhangls.entry

import kotlinx.serialization.Serializable
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.framework.mvi.MviState


/**
 * @author zhangls
 */
@Serializable
data class AppState(
  val isLogin: Boolean?,
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT,
  val fontSize: FontSizeConfig = FontSizeConfig.STANDARD,
  val appLanguage: AppLanguage? = null,
) : MviState
