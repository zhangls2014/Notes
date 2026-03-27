package me.zhangls.data.model

import kotlinx.serialization.Serializable


/**
 * 通用数据
 */
@Serializable
data class CommonModel(
  // 应用启动次数
  val launchCount: Long = 0,
  // 上一次启动时的版本号
  val lastVersionCode: Long = 0,
)
