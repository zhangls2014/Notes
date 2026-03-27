package me.zhangls.data.database

import androidx.room3.RoomDatabase


expect class AppDatabaseFactory {
  fun getDatabaseBuilder(databaseName: String): RoomDatabase.Builder<AppDatabase>
}
