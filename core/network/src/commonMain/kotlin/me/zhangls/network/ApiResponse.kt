package me.zhangls.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


/**
 * @author zhangls
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ApiResponse<T>(
  val code: Int,
  @JsonNames("message", "msg")
  val message: String,
  val data: T?
)