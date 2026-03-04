package me.zhangls.notes.ui

import kotlinx.serialization.Serializable
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.framework.mvi.MviState


/**
 * @author zhangls
 */
@Serializable
data class MainState(
  val isLogin: Boolean?,
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT,
) : MviState
