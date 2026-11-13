package me.zhangls.entry

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.repository.UserRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AccountSession(
  private val endpoint: String,
  private val users: UserRepository,
  private val rotateTokens: suspend (accountId: String, refreshToken: String, operationId: String) -> AuthTokens,
) {
  private val mutex = Mutex()
  private val inFlight = MutableStateFlow(0)
  private var generation = 0L

  internal class Snapshot(val accountId: String, val generation: Long, val tokens: AuthTokens)

  suspend fun snapshot(accountId: String): Snapshot = mutex.withLock { current(accountId) }

  suspend fun <T> establish(login: suspend () -> T): T = mutex.withLock {
    login().also { generation++ }
  }

  suspend fun clear(): AuthTokens? = mutex.withLock {
    val credentials = users.getUser()
      ?.takeIf { sessionMatchesAuthOrigin(it, endpoint) }
      ?.let { users.getTokens() }
    users.clear()
    generation++
    credentials
  }

  suspend fun <T> request(accountId: String, call: suspend (String) -> T): T = request(snapshot(accountId), call)

  suspend fun <T> request(session: Snapshot, call: suspend (String) -> T): T {
    val sent = beginRequest(session)
    val result = try {
      execute(sent, call)
    } catch (failure: AuthHttpException) {
      if (!failure.invalidSession) throw failure
      refresh(sent, failure)
      currentCoroutineContext().ensureActive()
      execute(beginRequest(sent), call)
    }
    mutex.withLock { matching(sent) }
    return result
  }

  private suspend fun beginRequest(session: Snapshot): Snapshot = mutex.withLock {
    matching(session).also { inFlight.update { count -> count + 1 } }
  }

  private suspend fun <T> execute(session: Snapshot, call: suspend (String) -> T): T = try {
    call(session.tokens.accessToken)
  } finally {
    inFlight.update { it - 1 }
  }

  @OptIn(ExperimentalUuidApi::class)
  private suspend fun refresh(sent: Snapshot, failure: AuthHttpException) = mutex.withLock {
    val current = matching(sent)
    if (current.tokens.accessToken != sent.tokens.accessToken) return@withLock
    val previous = current.tokens.refreshToken
    if (previous.isBlank()) throw failure

    // The server checks the original access token again when committing certain writes.
    // Pause new requests while the existing ones finish, then rotate the token once.
    inFlight.first { it == 0 }

    // Once rotation starts, finish persisting its result even if the initiating screen closes.
    withContext(NonCancellable) {
      val operationId = current.tokens.refreshOperationId ?: Uuid.random().toString().also { id ->
        val pending = AuthTokens(current.tokens.accessToken, previous, id)
        if (!users.replaceTokens(sent.accountId, previous, pending)) throw invalidSession()
      }
      val tokens = try {
        rotateTokens(sent.accountId, previous, operationId)
      } catch (rejected: AuthHttpException) {
        if (rejected.invalidSession && matching(sent).tokens.refreshToken == previous) {
          users.clear()
          generation++
        }
        throw rejected
      }
      if (!users.replaceTokens(sent.accountId, previous, tokens)) throw invalidSession()
    }
  }

  private suspend fun matching(session: Snapshot): Snapshot {
    if (session.generation != generation) throw invalidSession()
    return current(session.accountId)
  }

  private suspend fun current(accountId: String): Snapshot {
    val user = users.getUser() ?: throw invalidSession()
    if (user.id != accountId || !sessionMatchesAuthOrigin(user, endpoint)) throw invalidSession()
    return Snapshot(accountId, generation, users.getTokens() ?: throw invalidSession())
  }

  private fun invalidSession() = AuthHttpException(401, "invalid_session")
}
