package me.zhangls.data.impl.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import me.zhangls.database.AppDatabaseFactory
import me.zhangls.database.create
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RoomAccountDataRepositoryTest {
  @Test
  fun switchesAccountsWithoutSharingEditsOrRebindingOldRepositories() = runBlocking {
    val prefix = NSTemporaryDirectory() + "notes-account-" + NSUUID().UUIDString
    val openedFiles = mutableListOf<String>()
    val storage = RoomAccountDataRepository { filename ->
      openedFiles += filename
      AppDatabaseFactory().getDatabaseBuilder("$prefix-$filename").create()
    }
    val accountA = "bfc048f0-4d73-48aa-b4a4-57974ff0e314"
    val accountB = "d987730c-d6a7-4893-81ed-9ac0ce77415a"
    val messages = listOf(
      EmailModel(1, AccountModel(1, "Ada", "Notes", "", "", ""), subject = "Sample", body = "", createdAt = ""),
    )
    try {
      storage.open("local", messages.map { it.copy(id = 2, subject = "Legacy draft") })
      storage.open(accountA, messages)
      assertNull(storage.emails().getEmail(2).first())
      val oldEmails = storage.emails()
      oldEmails.toggleFavorite(1)
      val collected = CompletableDeferred<Unit>()
      val collector = launch {
        oldEmails.getEmail(1).collect { collected.complete(Unit) }
      }
      withTimeout(5_000) { collected.await() }
      storage.open(accountB, messages)
      withTimeout(5_000) { collector.join() }
      assertEquals(false, storage.emails().getEmail(1).first()?.isImportant)
      assertFailsWith<CancellationException> { oldEmails.deleteEmails(setOf(1L)) }
      storage.emails().deleteEmails(setOf(1L))
      storage.close()
      storage.open(accountB, messages)
      assertNull(storage.emails().getEmail(1).first())
      storage.open(accountA, messages)
      assertEquals(true, storage.emails().getEmail(1).first()?.isImportant)
      storage.open("local", messages)
      assertEquals("Legacy draft", storage.emails().getEmail(2).first()?.subject)
      assertEquals(
        listOf(
          "notes.db", "notes-$accountA.db", "notes-$accountB.db",
          "notes-$accountB.db", "notes-$accountA.db", "notes.db",
        ),
        openedFiles,
      )
    } finally {
      storage.close()
    }
  }

  @Test
  fun rejectsUntrustedAccountPathsBeforeCreatingDatabase() = runBlocking {
    val storage = RoomAccountDataRepository { error("Unexpected file: $it") }
    assertFailsWith<IllegalArgumentException> { storage.open("../notes", emptyList()) }
    Unit
  }
}
