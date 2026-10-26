package me.zhangls.model

import kotlinx.serialization.Serializable

private const val MEDIUM_FONT_SCALE = 1.5F

@Serializable
enum class FontSizeConfig(val value: Float) {
  STANDARD(1F),
  MEDIUM(MEDIUM_FONT_SCALE),
  LARGE(2F),
}
