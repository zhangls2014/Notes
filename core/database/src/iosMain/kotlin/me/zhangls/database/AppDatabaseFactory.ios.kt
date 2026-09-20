package me.zhangls.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.annotation.Factory


@Factory
actual class AppDatabaseFactory {
  actual fun getDatabaseBuilder(databasePath: String): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(name = databasePath)
  }
}
