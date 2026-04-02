package me.zhangls.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory


@Factory
actual class AppDatabaseFactory(private val manager: AppFileManager) {
  actual fun getDatabaseBuilder(databaseName: String): RoomDatabase.Builder<AppDatabase> {
    val filepath = manager.getDatabasePath(databaseName)
    return Room.databaseBuilder<AppDatabase>(name = filepath)
  }
}
