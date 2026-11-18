package me.zhangls.download

import kotlinx.serialization.Serializable

@Serializable
data class DownloadRequest(val url: String, val destination: String, val size: Long, val sha256: String)
