package me.zhangls.data.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.annotation.Factory


@Factory
actual class AppDatabaseFactory(private val context: Context) {
  actual fun getDatabaseBuilder(databaseName: String): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(databaseName)
    return Room.databaseBuilder<AppDatabase>(
      context = appContext,
      name = dbFile.absolutePath
    )
  }
}