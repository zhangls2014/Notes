package me.zhangls.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import me.zhangls.data.database.AppDatabase
import me.zhangls.data.database.createDatabase
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import me.zhangls.data.datastore.SettingsSerializer
import me.zhangls.data.datastore.UserSerializer
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.model.UserModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("me.zhangls.data")
class DataModule

@Singleton
fun provideDatabase(context: Context): AppDatabase {
  return createDatabase(context)
}

@Factory
fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

@Factory
fun provideEmailDao(database: AppDatabase): EmailDao = database.emailDao()

@Factory
@Named("SettingsDataStore")
fun provideSettingsModel(context: Context): DataStore<SettingsModel> = DataStoreFactory.create(
  corruptionHandler = ReplaceFileCorruptionHandler { SettingsModel() },
  serializer = SettingsSerializer(),
  produceFile = { context.dataStoreFile("settings.json") }
)

@Factory
@Named("UserDataStore")
fun provideUserModel(context: Context): DataStore<UserModel?> = DataStoreFactory.create(
  corruptionHandler = ReplaceFileCorruptionHandler { null },
  serializer = UserSerializer(),
  produceFile = { context.dataStoreFile("user.json") }
)