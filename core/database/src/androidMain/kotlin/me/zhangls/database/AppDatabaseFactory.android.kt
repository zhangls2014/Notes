package me.zhangls.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.annotation.Factory


@Factory
actual class AppDatabaseFactory(private val context: Context) {
  actual fun getDatabaseBuilder(databasePath: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
      context = context.applicationContext,
      name = databasePath
    )
  }
}