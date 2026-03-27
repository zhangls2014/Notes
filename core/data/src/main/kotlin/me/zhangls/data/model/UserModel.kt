package me.zhangls.data.model

import kotlinx.serialization.Serializable

/**
 * ⚠️ 新增字段需要添加默认值
 */
@Serializable
data class UserModel(
  val id: String,
  val nickname: String,
  val accessToken: String,
  val avatar: String? = null,
)
