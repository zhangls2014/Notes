package me.zhangls.database

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * 数据库层的 Koin 模块。
 *
 * 只登记本模块自己创建的东西——`AppDatabaseFactory`（平台建库方式）。
 * `AppDatabase` 的装配还需要数据库文件位置，而文件位置的唯一来源是 `core:data`
 * 的 `AppFileManager`（它同时被 `feature` 层使用，不能下沉到本模块），
 * 因此 `AppDatabase` 与各 DAO 的 provider 仍由 `core:data` 的 `DataModule` 提供。
 *
 * @author zhangls
 */
@Module
@ComponentScan("me.zhangls.database")
class DatabaseModule
