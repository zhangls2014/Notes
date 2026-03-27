package me.zhangls.settings

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module


@Module(includes = [DataModule::class, FrameworkModule::class])
@ComponentScan("me.zhangls.settings")
class SettingsModule