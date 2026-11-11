package me.zhangls.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.zhangls.data.impl.repository.RoomAccountDataRepository
import me.zhangls.data.repository.AccountDataRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.util.AppFileManager
import me.zhangls.database.AppDatabaseFactory
import me.zhangls.database.DatabaseModule
import me.zhangls.database.create
import okio.Path.Companion.toPath
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/**
 * 数据层的 Koin 模块。
 *
 * 数据库的创建方式由 [DatabaseModule] 提供，资料和会话存储保留独立的数据源。
 *
 * @author zhangls
 */
@Module(
  includes = [DatabaseModule::class],
)
@ComponentScan("me.zhangls.data")
class DataModule(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
  // 明确作为模块成员登记，避免 Native 跨模块顶层函数按返回类型包过滤。
  @Singleton
  internal fun provideIoDispatcher(): CoroutineDispatcher = ioDispatcher
}

@Singleton(binds = [AccountDataRepository::class])
internal fun provideAccountDataRepository(
  factory: AppDatabaseFactory,
  manager: AppFileManager,
  ioDispatcher: CoroutineDispatcher,
): RoomAccountDataRepository = RoomAccountDataRepository { filename ->
  factory.getDatabaseBuilder(manager.getDatabasePath(filename)).create(ioDispatcher)
}

/** A newly constructed feature ViewModel captures the current account, never a moving delegate. */
@Factory
internal fun provideEmailsRepository(accountData: RoomAccountDataRepository): EmailsRepository = accountData.emails()

@Singleton
internal fun provideDataStore(manager: AppFileManager): DataStore<Preferences> =
  PreferenceDataStoreFactory.createWithPath {
    manager.getDataStorePath("notes.preferences_pb").toPath()
  }
