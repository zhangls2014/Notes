package me.zhangls.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun emailDao(): EmailDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
  override fun initialize(): AppDatabase
}

internal fun RoomDatabase.Builder<AppDatabase>.create(): AppDatabase {
  return this.setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
}
