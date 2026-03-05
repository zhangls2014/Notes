package me.zhangls.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.model.UserModel
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author zhangls
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

  @Provides
  @Singleton
  @Named("SettingsDataStore")
  fun provideSettingsModel(@ApplicationContext context: Context): DataStore<SettingsModel> = DataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { SettingsModel() },
    serializer = SettingsSerializer(),
    produceFile = { context.dataStoreFile("settings.json") }
  )

  @Provides
  @Singleton
  @Named("UserDataStore")
  fun provideUserModel(@ApplicationContext context: Context): DataStore<UserModel?> = DataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { null },
    serializer = UserSerializer(),
    produceFile = { context.dataStoreFile("user.json") }
  )
}
