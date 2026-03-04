package me.zhangls.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .build()
  }

  @Provides
  fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

  @Provides
  fun provideEmailDao(database: AppDatabase): EmailDao = database.emailDao()
}