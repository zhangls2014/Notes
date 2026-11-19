package me.zhangls.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.AccountDataRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.MailRemote
import me.zhangls.data.repository.MailRequestException

internal class RemoteAccountDataRepository(private val remote: MailRemote) : AccountDataRepository {
  private var active: RemoteEmailsRepository? = null
  private var accountId: String? = null
  private val mutex = Mutex()

  override suspend fun open(accountId: String, initialEmails: List<EmailModel>) = mutex.withLock {
    if (this.accountId != accountId) {
      active?.store?.retire()
      active = RemoteEmailsRepository(RemoteMailStore(accountId, remote))
      this.accountId = accountId
    }
  }

  override suspend fun close() = mutex.withLock {
    active?.store?.retire()
    active = null
    accountId = null
  }

  fun emails(): EmailsRepository = checkNotNull(active)
}

private class RemoteEmailsRepository(val store: RemoteMailStore) : EmailsRepository {
  override suspend fun insertEmails(emails: List<EmailModel>) {
    error("Server owns mail initialization")
  }

  override suspend fun insertDraft(draft: EmailDraft) {
    store.call(
      "send",
      buildJsonObject {
        put("operationId", draft.operationId)
        put("recipientIds", JsonArray(draft.recipientIds.map { JsonPrimitive(it) }))
        put("subject", draft.subject)
        put("body", draft.body)
        put("mode", draft.mode)
        draft.sourceId?.let { put("sourceId", it) }
      },
    )
    store.changed()
  }

  override fun getEmail(id: Long): Flow<EmailModel?> = store.revision.map {
    try {
      store.json.decodeFromString<EmailModel>(store.call("detail", buildJsonObject { put("id", id) }))
    } catch (failure: MailRequestException) {
      if (failure.status == 404) null else throw failure
    }
  }

  override suspend fun toggleFavorite(emailId: Long) = store.mutate("favorite", setOf(emailId))

  override suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) =
    store.mutate("favorite", emailIds, isImportant)

  override suspend fun deleteEmails(emailIds: Set<Long>) = store.mutate("trash", emailIds)

  override fun getAllAccounts(): Flow<List<AccountModel>> = store.revision.map {
    store.json.decodeFromString<List<AccountModel>>(store.call("contacts")).filterNot { it.isDefault }
  }

  override fun getEmailPaging() = store.paging()

  override fun getEmailFavoritePaging() = store.paging(buildJsonObject { put("favorite", true) })

  override fun getThreadEmailsById(parentEmailId: Long) =
    store.paging(buildJsonObject { put("threadId", parentEmailId) })

  override fun searchEmails(keywords: String) = store.paging(buildJsonObject { put("query", keywords) })
}

/** Owns the fixed account lifetime and invalidates all its consumers after a successful write. */
private class RemoteMailStore(private val accountId: String, private val remote: MailRemote) {
  val json = Json { ignoreUnknownKeys = true }
  val revision = MutableStateFlow(0)
  private val lifetime = Job()

  fun retire() {
    lifetime.cancel()
  }

  suspend fun call(action: String, fields: JsonObject = buildJsonObject {}): String = coroutineScope {
    check(lifetime.isActive) { "Account mail store is retired" }
    val operation = coroutineContext[Job]!!
    val hook = lifetime.invokeOnCompletion { operation.cancel() }
    try {
      remote.request(
        accountId,
        buildJsonObject {
          put("action", action)
          fields.forEach { (key, value) -> put(key, value) }
        },
      )
    } finally {
      hook.dispose()
    }
  }

  fun changed() {
    revision.update { it + 1 }
  }

  suspend fun mutate(action: String, ids: Set<Long>, important: Boolean? = null) {
    if (ids.isEmpty()) return
    call(
      action,
      buildJsonObject {
        put("ids", JsonArray(ids.map { JsonPrimitive(it) }))
        important?.let { put("important", it) }
      },
    )
    changed()
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun paging(filter: JsonObject = buildJsonObject {}) = revision.flatMapLatest {
    Pager(PagingConfig(pageSize = MAIL_PAGE_SIZE, initialLoadSize = MAIL_PAGE_SIZE)) {
      RemoteMailPagingSource(this, filter)
    }.flow
  }
}

private const val MAIL_PAGE_SIZE = 20

private class RemoteMailPagingSource(
  private val store: RemoteMailStore,
  private val filter: JsonObject,
) : PagingSource<Int, EmailModel>() {
  override fun getRefreshKey(state: PagingState<Int, EmailModel>): Int = 0

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EmailModel> = try {
    val offset = params.key ?: 0
    val items = store.json.decodeFromString<List<EmailModel>>(
      store.call(
        "list",
        buildJsonObject {
          filter.forEach { (key, value) -> put(key, value) }
          put("offset", offset)
        },
      ),
    )
    LoadResult.Page(items, null, if (items.size < MAIL_PAGE_SIZE) null else offset + items.size)
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (failure: MailRequestException) {
    LoadResult.Error(failure)
  } catch (failure: SerializationException) {
    LoadResult.Error(failure)
  }
}
