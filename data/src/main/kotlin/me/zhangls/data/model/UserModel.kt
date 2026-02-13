package me.zhangls.data.model

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class UserModel(
  val id: String,
  val nickname: String,
  val accessToken: String
)
