package me.zhangls.data.impl.repository

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.MailRemote
import me.zhangls.data.repository.MailRequestException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RemoteMailTest {
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun pagingAppendsRetriesStopsAndRefreshesFromFirstPage() = runTest {
    val offsets = mutableListOf<Int>()
    var failAppend = true
    var subject = "Original"
    val stores = RemoteAccountDataRepository(object : MailRemote {
      override suspend fun request(accountId: String, input: JsonObject): String {
        val offset = input["offset"]!!.jsonPrimitive.content.toInt()
        offsets += offset
        if (offset == 20 && failAppend) throw MailRequestException(IllegalStateException("Offline"))
        val emails = (offset until minOf(offset + 20, 45)).map {
          EmailModel(
            id = it.toLong() + 1,
            sender = AccountModel(1, "Ada", "", "ada@example.invalid", "", "avatar_1"),
            subject = subject,
            body = "Body",
            createdAt = "09:30",
          )
        }
        return Json.encodeToString(
          ListSerializer(EmailModel.serializer()),
          emails,
        )
      }
    })
    stores.open("alice", emptyList())
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    val presenter = object : PagingDataPresenter<EmailModel>(dispatcher) {
      override suspend fun presentPagingDataEvent(event: PagingDataEvent<EmailModel>) = Unit
    }
    backgroundScope.launch(dispatcher) {
      stores.emails().getEmailPaging().collectLatest { presenter.collectFrom(it) }
    }
    runCurrent()
    assertEquals(20, presenter.size)
    presenter[19]
    runCurrent()
    assertEquals(listOf(0, 20), offsets)
    assertTrue(presenter.loadStateFlow.value?.append is LoadState.Error)
    assertEquals(20, presenter.size)
    failAppend = false
    presenter.retry()
    runCurrent()
    assertEquals(40, presenter.size)
    presenter[39]
    runCurrent()
    assertEquals(45, presenter.size)
    assertEquals((1L..45L).toList(), presenter.snapshot().items.map { it.id })
    presenter[44]
    runCurrent()
    assertEquals(listOf(0, 20, 20, 40), offsets)
    subject = "Refreshed"
    presenter.refresh()
    runCurrent()
    assertEquals(0, offsets.last())
    assertEquals("Refreshed", presenter.snapshot().items.first().subject)
    stores.close()
  }

  @Test
  fun switchingAccountsRetiresCapturedRepositoryAndNeverUploadsSeeds() = runTest {
    val calls = mutableListOf<Pair<String, JsonObject>>()
    val stores = RemoteAccountDataRepository(object : MailRemote {
      override suspend fun request(accountId: String, input: JsonObject): String {
        calls.add(accountId to input)
        return "[]"
      }
    })
    stores.open("alice", emptyList())
    val alice = stores.emails()
    stores.open("alice", emptyList())
    assertSame(alice, stores.emails())
    alice.getAllAccounts().first()
    assertEquals("alice", calls.single().first)
    stores.open("bob", emptyList())
    assertFailsWith<IllegalStateException> { alice.getAllAccounts().first() }
    val bob = stores.emails()
    bob.insertDraft(EmailDraft(setOf(2), "Subject", "Body", "", "operation", "reply", 42))
    assertEquals("bob", calls.last().first)
    assertEquals("operation", calls.last().second["operationId"]?.jsonPrimitive?.content)
    assertEquals("42", calls.last().second["sourceId"]?.jsonPrimitive?.content)
    stores.close()
    assertFailsWith<IllegalStateException> { bob.getAllAccounts().first() }
  }

  @Test
  fun detailMapsNotFoundToMissingButPreservesServerFailures() = runTest {
    var status = 404
    val stores = RemoteAccountDataRepository(object : MailRemote {
      override suspend fun request(accountId: String, input: JsonObject): String {
        throw MailRequestException(IllegalStateException("HTTP error"), status)
      }
    })
    stores.open("alice", emptyList())
    assertNull(stores.emails().getEmail(1).first())
    status = 500
    assertFailsWith<MailRequestException> { stores.emails().getEmail(1).first() }
    stores.close()
  }

}
