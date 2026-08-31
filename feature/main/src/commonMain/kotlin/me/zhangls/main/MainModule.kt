package me.zhangls.main

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.network.NetworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module


// EmailModule 由组合根（composeApp）引入；main 仅依赖 email 契约（:feature:email-api）
@Module(
  includes = [
    DataModule::class,
    NetworkModule::class,
    FrameworkModule::class
  ]
)
@ComponentScan("me.zhangls.main")
class MainModule
