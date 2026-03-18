package me.zhangls.notes

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.login.LoginModule
import me.zhangls.main.MainModule
import me.zhangls.network.NetworkModule
import me.zhangls.notes.data.api.JokesApi
import me.zhangls.settings.SettingsModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import retrofit2.Retrofit

/**
 * @author zhangls
 */
@Module(
  includes = [
    DataModule::class,
    FrameworkModule::class,
    NetworkModule::class,
    LoginModule::class,
    MainModule::class,
    SettingsModule::class
  ]
)
@ComponentScan("me.zhangls.notes")
class NotesModule

@Factory
fun provideJokesApi(retrofit: Retrofit): JokesApi = retrofit.create(JokesApi::class.java)
