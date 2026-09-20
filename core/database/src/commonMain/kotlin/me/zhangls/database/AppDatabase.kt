package me.zhangls.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.ColumnTypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.zhangls.database.dao.AccountDao
import me.zhangls.database.dao.EmailDao
import me.zhangls.database.entity.AccountEntity
import me.zhangls.database.entity.EmailEntity

@Database(
  entities = [
    AccountEntity::class,
    EmailEntity::class,
  ],
  // 警告⚠️：每次数据库字段变化都需要配置自动迁移，否则需要面临数据丢失风险！
  version = 4,
  exportSchema = true,
  autoMigrations = [
    AutoMigration(from = 1, to = 2),
    AutoMigration(from = 2, to = 3),
    AutoMigration(from = 3, to = 4),
  ]
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun emailDao(): EmailDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
  override fun initialize(): AppDatabase
}

/**
 * 用项目统一的 SQLite 驱动与 IO 调度器完成建库。
 *
 * 供 `core:data` 装配数据库时调用；驱动选择属于本模块的存储细节。
 */
fun RoomDatabase.Builder<AppDatabase>.create(): AppDatabase {
  return this.setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
}
