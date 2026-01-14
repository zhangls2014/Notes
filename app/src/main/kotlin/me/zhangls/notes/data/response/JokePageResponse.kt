package me.zhangls.notes.data.response

import kotlinx.serialization.Serializable
import me.zhangls.notes.data.model.JokeModel

/**
 * @author zhangls
 */
@Serializable
data class JokePageResponse(
  val page: Int,
  val totalCount: Int,
  val totalPage: Int,
  val limit: Int,
  val list: List<JokeModel>
)