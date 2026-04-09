package me.zhangls.main

import me.zhangls.data.DataModule
import me.zhangls.email.EmailModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.network.NetworkModule
import me.zhangls.settings.SettingsModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module


@Module(
  includes = [
    DataModule::class,
    NetworkModule::class,
    FrameworkModule::class,
    SettingsModule::class,
    EmailModule::class
  ]
)
@ComponentScan("me.zhangls.main")
class MainModule
