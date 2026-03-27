package me.zhangls.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import me.zhangls.data.documentDirectory
import org.koin.core.annotation.Factory


@Factory
actual class AppDatabaseFactory {
  actual fun getDatabaseBuilder(databaseName: String): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = "${documentDirectory()}/database/${databaseName}"
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
  }
}
