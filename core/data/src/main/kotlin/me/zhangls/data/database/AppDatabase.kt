package me.zhangls.data.database

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import kotlinx.coroutines.Dispatchers
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import me.zhangls.data.database.entity.AccountEntity
import me.zhangls.data.database.entity.EmailEntity

@Database(
  entities = [
    AccountEntity::class,
    EmailEntity::class,
  ],
  // 警告⚠️：每次数据库字段变化都需要配置自动迁移，否则需要面临数据丢失风险！
  version = 1,
  exportSchema = true,
  autoMigrations = []
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun emailDao(): EmailDao
}

internal fun createDatabase(context: Context): AppDatabase {
  return Room.databaseBuilder(context, AppDatabase::class.java, "notes")
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
}
