package me.zhangls.notes.data.model

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class JokeModel(
  val content: String,
  val updateTime: String
)