package me.zhangls.notes.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.zhangls.notes.data.model.SettingsModel
import me.zhangls.notes.data.model.UserModel
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
    serializer = SettingsSerializer(),
    produceFile = { context.dataStoreFile("settings.json") }
  )

  @Provides
  @Singleton
  @Named("UserDataStore")
  fun provideUserModel(@ApplicationContext context: Context): DataStore<UserModel> = DataStoreFactory.create(
    serializer = UserSerializer(),
    produceFile = { context.dataStoreFile("user.json") }
  )
}