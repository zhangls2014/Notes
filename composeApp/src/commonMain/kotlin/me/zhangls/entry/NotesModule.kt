package me.zhangls.entry

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.login.LoginModule
import me.zhangls.main.MainModule
import me.zhangls.settings.SettingsModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * @author zhangls
 */
@Module(
  includes = [
    DataModule::class,
    FrameworkModule::class,
    LoginModule::class,
    MainModule::class,
    SettingsModule::class
  ]
)
@ComponentScan("me.zhangls.entry")
class NotesModule