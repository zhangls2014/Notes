package me.zhangls.email

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module


@Module(includes = [DataModule::class, FrameworkModule::class])
@ComponentScan("me.zhangls.email")
class EmailModule
