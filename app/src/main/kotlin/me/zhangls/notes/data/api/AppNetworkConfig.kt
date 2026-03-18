package me.zhangls.notes.data.api

import me.zhangls.network.NetworkConfig
import me.zhangls.notes.BuildConfig
import org.koin.core.annotation.Singleton


@Singleton(binds = [NetworkConfig::class])
class AppNetworkConfig : NetworkConfig {
  override val baseUrl = BuildConfig.BASE_URL
  override val isDebug: Boolean = BuildConfig.DEBUG
}