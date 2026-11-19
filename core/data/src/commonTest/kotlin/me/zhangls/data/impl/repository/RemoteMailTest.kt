package me.zhangls.data.impl.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.repository.MailRequestException
import me.zhangls.data.repository.MailRemote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertNull

class RemoteMailTest {
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
