@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.database

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseMigrationTest {
  @Test fun migratesVersion1WithoutLosingData() = verifyMigration(1)
  @Test fun migratesVersion2WithoutLosingData() = verifyMigration(2)
  @Test fun migratesVersion3WithoutLosingData() = verifyMigration(3)

  private fun verifyMigration(version: Int) = runBlocking {
    val directory = NSTemporaryDirectory() + "notes-migration-" + NSUUID().UUIDString
    val path = "$directory/notes.db"
    val files = NSFileManager.defaultManager
    check(files.createDirectoryAtPath(directory, true, null, null))
    try {
      BundledSQLiteDriver().open(path).use { connection ->
        fun execute(sql: String) = connection.prepare(sql).use { it.step(); Unit }
        historicalSchemas.getValue(version).forEach(::execute)
        val defaultColumn = if (version >= 3) ", isDefault" else ""
        val defaultValue = if (version >= 3) ", 1" else ""
        execute("INSERT INTO account (id, firstName, lastName, email, altEmail, avatar$defaultColumn) VALUES (1, 'Ada', 'Lovelace', 'ada@example.com', '', ''$defaultValue)")
        val recipientsColumn = if (version >= 2) ", recipientIds" else ""
        val recipientsValue = if (version >= 2) ", '[2,3]'" else ""
        execute("INSERT INTO email (id, senderId, subject, body, isImportant, isStarred, mailbox, createdAt$recipientsColumn) VALUES (7, 1, 'Keep this message', 'Historical body', 1, 0, 0, '2026-01-01'$recipientsValue)")
      }
      val database = AppDatabaseFactory().getDatabaseBuilder(path).create()
      try {
        val account = assertNotNull(database.accountDao().queryDefaultAccount())
        assertEquals("ada@example.com", account.email)
        assertEquals(version >= 3, account.isDefault)
        val email = assertNotNull(database.emailDao().getEmailById(7))
        assertEquals("Keep this message", email.subject)
        assertEquals("Historical body", email.body)
        assertEquals(true, email.isImportant)
        assertEquals(if (version >= 2) "[2,3]" else "[]", email.recipientIds)
      } finally {
        database.close()
      }
      BundledSQLiteDriver().open(path).use { connection ->
        connection.prepare("PRAGMA user_version").use {
          check(it.step())
          assertEquals(4L, it.getLong(0))
        }
      }
    } finally {
      check(files.removeItemAtPath(directory, null))
    }
  }
}
