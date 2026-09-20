package me.zhangls.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import me.zhangls.data.util.AppFileManager
import me.zhangls.database.AppDatabase
import me.zhangls.database.AppDatabaseFactory
import me.zhangls.database.DatabaseModule
import me.zhangls.database.create
import me.zhangls.database.dao.AccountDao
import me.zhangls.database.dao.EmailDao
import okio.Path.Companion.toPath
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/**
 * 数据层的 Koin 模块。
 *
 * 组合 [DatabaseModule]（平台建库方式）—— 本模块负责数据库文件位置（[AppFileManager]），
 * 两者拼出 `AppDatabase`。
 *
 * 以下 provider 全部为 `internal`：它们引用了 `core:database` 的公开类型
 * （`AppDatabase` / `AccountDao` / `EmailDao`），收成 `internal` 可避免这些类型
 * 出现在 `core:data` 的公开 API 上。
 *
 * @author zhangls
 */
@Module(
  includes = [DatabaseModule::class]
)
@ComponentScan("me.zhangls.data")
class DataModule

@Singleton
internal fun provideDatabase(factory: AppDatabaseFactory, manager: AppFileManager): AppDatabase {
  return factory.getDatabaseBuilder(manager.getDatabasePath(DATABASE_NAME)).create()
}

@Factory
internal fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

@Factory
internal fun provideEmailDao(database: AppDatabase): EmailDao = database.emailDao()

@Singleton
internal fun provideDataStore(manager: AppFileManager): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
  manager.getDataStorePath("notes.preferences_pb").toPath()
}

private const val DATABASE_NAME = "notes.db"

