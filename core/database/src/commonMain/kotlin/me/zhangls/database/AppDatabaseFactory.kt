package me.zhangls.database

import androidx.room3.RoomDatabase

/**
 * 建库工厂：把「平台差异」与「数据库文件位置」解耦。
 *
 * 本工厂只负责平台差异（Android 建库需要 `Context`，iOS 直接按绝对路径建库）；
 * 文件位置由调用方传入，其唯一来源是 `core:data` 的 `AppFileManager`。
 *
 * @author zhangls
 */
expect class AppDatabaseFactory {
  /** @param databasePath 数据库文件的绝对路径。 */
  fun getDatabaseBuilder(databasePath: String): RoomDatabase.Builder<AppDatabase>
}
