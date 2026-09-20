package me.zhangls.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import me.zhangls.data.impl.database.AppDatabase
import me.zhangls.data.impl.database.AppDatabaseFactory
import me.zhangls.data.impl.database.create
import me.zhangls.data.impl.database.dao.AccountDao
import me.zhangls.data.impl.database.dao.EmailDao
import me.zhangls.data.util.AppFileManager
import okio.Path.Companion.toPath
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("me.zhangls.data")
class DataModule

// AppDatabaseFactory 为 internal，故本 provider 也必须为 internal
@Singleton
internal fun provideDatabase(factory: AppDatabaseFactory): AppDatabase {
  return factory.getDatabaseBuilder("notes.db").create()
}

@Factory
fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

@Factory
fun provideEmailDao(database: AppDatabase): EmailDao = database.emailDao()

@Singleton
fun provideDataStore(manager: AppFileManager): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
  manager.getDataStorePath("notes.preferences_pb").toPath()
}
