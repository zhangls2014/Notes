package me.zhangls.entry

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.PasskeySessionVerificationRequiredException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AccountSessionTest {
  @Test
  fun requestsRunInParallelAndConcurrentUnauthorizedResponsesRefreshOnce() = runTest {
    val users = SessionUsers()
    var refreshes = 0
    val session = AccountSession(origin, users) { _, _, _ ->
      refreshes++
      AuthTokens("renewed", "rotated")
    }
    val firstStarted = CompletableDeferred<Unit>()
    val secondStarted = CompletableDeferred<Unit>()
    val releaseFirst = CompletableDeferred<Unit>()

    val first = async {
      session.request(accountId) {
        if (it == "initial") {
          firstStarted.complete(Unit)
          releaseFirst.await()
          throw invalidSession()
        }
        it
      }
    }
    firstStarted.await()
    val second = async {
      session.request(accountId) {
        if (it == "initial") {
          secondStarted.complete(Unit)
          throw invalidSession()
        }
        it
      }
    }
    secondStarted.await()
    assertEquals(0, refreshes)
    releaseFirst.complete(Unit)
    assertEquals("renewed", first.await())
    assertEquals("renewed", second.await())
    assertEquals(1, refreshes)
  }

  @Test
  fun previousSessionCannotRefreshOrReplayAfterSigningBackIntoSameAccount() = runTest {
    val users = SessionUsers()
    val session = AccountSession(origin, users) { _, _, _ -> error("Stale request must not refresh") }
    val started = CompletableDeferred<Unit>()
    val finish = CompletableDeferred<Unit>()
    val pending = async {
      assertFailsWith<AuthHttpException> {
        session.request(accountId) {
          assertEquals("initial", it)
          started.complete(Unit)
          finish.await()
          throw invalidSession()
        }
      }
    }
    started.await()
    session.clear()
    session.establish { users.login(account, AuthTokens("new-session", "new-refresh")) }
    finish.complete(Unit)
    assertTrue(pending.await().invalidSession)
    assertEquals("new-session", session.request(accountId) { it })
  }

  @Test
  fun failedRefreshCanRecoverWithSamePersistedOperationAfterRecreation() = runTest {
    val users = SessionUsers()
    var originalOperation: String? = null
    val interrupted = AccountSession(origin, users) { _, _, operation ->
      assertEquals(operation, users.getTokens()?.refreshOperationId)
      originalOperation = operation
      throw io.ktor.client.plugins.HttpRequestTimeoutException(origin, 10)
    }
    assertFailsWith<io.ktor.client.plugins.HttpRequestTimeoutException> {
      interrupted.request(accountId) { throw invalidSession() }
    }
    val recreated = AccountSession(origin, users) { _, token, operation ->
      assertEquals("initial-refresh", token)
      assertEquals(originalOperation, operation)
      AuthTokens("recovered", "new-refresh")
    }
    assertEquals("recovered", recreated.request(accountId) {
      if (it == "initial") throw invalidSession()
      it
    })
    assertEquals(null, users.getTokens()?.refreshOperationId)
  }

  @Test
  fun nonSessionErrorsDoNotRefreshOrReplay() = runTest {
    val session = AccountSession(origin, SessionUsers()) { _, _, _ -> error("Unexpected refresh") }
    for (failure in listOf(
      AuthHttpException(401, "invalid_passkey"),
      AuthHttpException(403, "recent_verification_required"),
      AuthHttpException(500, "internal_error"),
      io.ktor.client.plugins.HttpRequestTimeoutException("https://auth.test.example", 10),
    )) {
      var calls = 0
      val thrown = assertFailsWith<Exception> { session.request(accountId) { calls++; throw failure } }
      assertEquals(failure, thrown)
      assertEquals(1, calls)
    }
  }

  @Test
  fun onlyRecentVerificationForbiddenPromptsForVerification() = runTest {
    var code = "recent_verification_required"
    val engine = object : HttpClientEngineBase("AuthTest") {
      override val config = HttpClientEngineConfig()

      @OptIn(InternalAPI::class)
      override suspend fun execute(data: HttpRequestData) = HttpResponseData(
        HttpStatusCode.Forbidden,
        GMTDate(),
        Headers.Empty,
        HttpProtocolVersion.HTTP_1_1,
        ByteReadChannel("""{"error":"$code"}"""),
        callContext(),
      )
    }
    try {
      HttpClient(engine).use { http ->
        val auth = PasswordlessAuthClient(origin, SessionUsers(), http) {}
        val management = AccountPasskeyManagement(auth, provider = null)
        assertFailsWith<PasskeySessionVerificationRequiredException> { management.list() }
        code = "forbidden"
        val failure = assertFailsWith<AuthHttpException> { management.list() }
        assertEquals(403, failure.status)
        assertEquals("forbidden", failure.code)
      }
    } finally {
      engine.close()
    }
  }

  @Test
  fun jsonAndAvatarHttpFailuresKeepStatusCodeAndRequestId() = runTest {
    val requestId = "09876543-abcd-1234-abcd-0123456789ab"
    val engine = object : HttpClientEngineBase("AuthTest") {
      override val config = HttpClientEngineConfig()
      @OptIn(InternalAPI::class) // Script responses through Ktor using the existing test classpath.
      override suspend fun execute(data: HttpRequestData) = HttpResponseData(
        HttpStatusCode.InternalServerError,
        GMTDate(),
        Headers.build { append("X-Request-Id", requestId) },
        HttpProtocolVersion.HTTP_1_1,
        ByteReadChannel("""{"error":"internal_error"}"""),
        callContext(),
      )
    }
    try {
      HttpClient(engine).use { http ->
        val diagnostics = mutableListOf<String>()
        val client = PasswordlessAuthClient(origin, SessionUsers(), http) { diagnostics.add(it) }
        val listFailure = assertFailsWith<AuthHttpException> { client.listPasskeys() }
        val avatarFailure = assertFailsWith<AuthHttpException> { client.getAvatar(accountId) }
        for (failure in listOf(listFailure, avatarFailure)) {
          assertEquals(500, failure.status)
          assertEquals("internal_error", failure.code)
          assertEquals(requestId, failure.requestId)
        }
        assertEquals(List(2) { "HTTP status=500 code=internal_error requestId=$requestId" }, diagnostics)
      }
    } finally {
      engine.close()
    }
  }
}

private const val origin = "https://auth.test.example"
private const val accountId = "test-account"
private val account = UserModel(accountId, "Example", authOrigin = origin)

private fun invalidSession() = AuthHttpException(401, "invalid_session")

private class SessionUsers : UserRepository {
  override val userFlow = MutableStateFlow<UserModel?>(account)
  private var tokens: AuthTokens? = AuthTokens("initial", "initial-refresh")

  override suspend fun getUser() = userFlow.value
  override suspend fun getTokens() = tokens
  override suspend fun login(user: UserModel, tokens: AuthTokens) {
    this.tokens = tokens
    userFlow.value = user
  }

  override suspend fun replaceTokens(userId: String, expectedRefreshToken: String, tokens: AuthTokens): Boolean {
    if (userFlow.value?.id != userId || this.tokens?.refreshToken != expectedRefreshToken) return false
    this.tokens = tokens
    return true
  }

  override suspend fun clear() {
    tokens = null
    userFlow.value = null
  }

  override suspend fun updateAvatar(avatar: String, expectedUser: UserModel) = error("Unexpected call")
  override suspend fun updateAccountProfile(
    account: String,
    avatar: String?,
    revision: String?,
    expectedUser: UserModel,
    email: String?,
  ) = error("Unexpected call")
  override suspend fun updateBiometricUnlock(enabled: Boolean) = error("Unexpected call")
  override suspend fun updateEmailSearchHistory(keyword: String): Unit = error("Unexpected call")
  override suspend fun deleteEmailSearchHistory(keyword: String): Unit = error("Unexpected call")
}
