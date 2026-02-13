package me.zhangls.data.model

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class SettingsModel(
  // 是否为深色模式。null: 跟随系统，true: 深色模式，false: 浅色模式
  val isDarkMode: Boolean? = false
)
