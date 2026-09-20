package me.zhangls.data.impl.database

import androidx.room3.RoomDatabase


internal expect class AppDatabaseFactory {
  fun getDatabaseBuilder(databaseName: String): RoomDatabase.Builder<AppDatabase>
}
