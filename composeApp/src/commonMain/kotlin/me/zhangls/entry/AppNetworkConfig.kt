package me.zhangls.entry

import me.zhangls.network.NetworkConfig
import org.koin.core.annotation.Singleton

@Singleton(binds = [NetworkConfig::class])
class AppNetworkConfig : NetworkConfig {
  override val baseUrl = BuildKonfig.BASE_URL
}