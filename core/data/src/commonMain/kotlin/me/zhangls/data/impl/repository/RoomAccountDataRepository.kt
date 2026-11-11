package me.zhangls.data.impl.repository

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.AccountDataRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.database.AppDatabase
import kotlin.concurrent.Volatile

internal class RoomAccountDataRepository(
  private val createDatabase: (filename: String) -> AppDatabase,
) : AccountDataRepository {
  private val mutex = Mutex()
  @Volatile private var active: AccountStore? = null

  override suspend fun open(accountId: String, initialEmails: List<EmailModel>) = mutex.withLock {
    val filename = accountDatabaseName(accountId)
    if (active?.accountId == accountId) return@withLock
    retireActiveStore()
    val repository = EmailsRepositoryImpl(createDatabase(filename))
    try {
      repository.seedIfEmpty(initialEmails)
      currentCoroutineContext().ensureActive()
      active = AccountStore(accountId, repository)
    } catch (failure: Throwable) {
      repository.retire()
      throw failure
    }
  }

  override suspend fun close() = mutex.withLock {
    retireActiveStore()
  }

  fun emails(): EmailsRepository = checkNotNull(active) { "Account mail store is not ready" }.repository

  private fun retireActiveStore() {
    val store = active
    active = null
    store?.repository?.retire()
  }

  private class AccountStore(val accountId: String, val repository: EmailsRepositoryImpl)
}

private val accountIdPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

private fun accountDatabaseName(accountId: String): String {
  if (accountId == "local") return "notes.db"
  require(accountIdPattern.matches(accountId)) { "Invalid server account ID" }
  return "notes-$accountId.db"
}
