package me.zhangls.data.type

import kotlinx.serialization.Serializable

@Serializable
enum class FontSizeConfig(val value: Float) {
  STANDARD(1F),
  MEDIUM(1.5F),
  LARGE(2F),
}