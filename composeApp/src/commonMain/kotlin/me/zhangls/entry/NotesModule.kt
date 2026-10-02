package me.zhangls.entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhangls.data.DataModule
import me.zhangls.about.AboutModule
import me.zhangls.entry.data.InitData
import me.zhangls.email.EmailModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.login.LoginModule
import me.zhangls.main.MainModule
import me.zhangls.settings.SettingsModule
import me.zhangls.profile.ProfileModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform
import org.koin.plugin.module.dsl.startKoin

/**
 * @author zhangls
 */
@Module(
  includes = [
    DataModule::class,
    FrameworkModule::class,
    EmailModule::class,
    LoginModule::class,
    MainModule::class,
    SettingsModule::class,
    ProfileModule::class,
    AboutModule::class
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

private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun initData() {
  val task = KoinPlatform.getKoin().get<InitData>()
  startupScope.launch { task.run() }
}
