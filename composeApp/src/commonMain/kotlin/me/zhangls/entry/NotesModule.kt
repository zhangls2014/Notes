package me.zhangls.entry

import me.zhangls.data.DataModule
import me.zhangls.entry.data.InitData
import me.zhangls.framework.FrameworkModule
import me.zhangls.login.LoginModule
import me.zhangls.main.MainModule
import me.zhangls.settings.SettingsModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

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


@KoinApplication(modules = [NotesModule::class])
@ComponentScan("me.zhangls.notes")
class NotesApp


fun initKoin(config: KoinAppDeclaration?) {
  startKoin<NotesApp>(config)
}

fun initData() {
  InitData.launch()
}
