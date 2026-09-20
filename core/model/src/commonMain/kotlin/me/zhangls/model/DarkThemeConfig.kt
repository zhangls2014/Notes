package me.zhangls.model

import kotlinx.serialization.Serializable

/**
 * 深色主题配置。
 *
 * 与 [AppLanguage] 同属跨层共享类型：设置页与登录页都需要展示它，
 * 而其 UI 数据（标题 / 选项文案 / 图标）的宿主 `core:theme` 作为 UI 基础模块
 * 不依赖数据层，故与 [AppLanguage] 一样下沉到 core:model。
 */
@Serializable
enum class DarkThemeConfig {
  FOLLOW_SYSTEM,
  LIGHT,
  DARK,
}
