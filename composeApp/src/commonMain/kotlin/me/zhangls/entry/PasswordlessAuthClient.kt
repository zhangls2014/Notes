package me.zhangls.entry

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult
import me.zhangls.login.api.PasskeyUnavailableException
import me.zhangls.login.api.PasswordlessAuth
import me.zhangls.network.httpClient

internal val configuredAuthEndpoint = passwordlessEndpoint(BuildKonfig.AUTH_BASE_URL)

internal fun passwordlessEndpoint(config: String): String? {
  if (config.isBlank()) return null
  val url = Url(config)
  require(
    url.protocol == URLProtocol.HTTPS && url.host.isNotBlank() && url.user == null && url.password == null &&
      url.encodedPath in listOf("", "/") && url.parameters.isEmpty() && url.fragment.isEmpty(),
  ) { "Notes authentication requires an HTTPS origin" }
  return config.trimEnd('/')
}

internal fun sessionMatchesAuthOrigin(user: UserModel, endpoint: String?): Boolean =
  if (endpoint == null) user.authOrigin == null else user.id != "local" && user.authOrigin == endpoint

internal class PasswordlessAuthClient(private val endpoint: String, private val users: UserRepository) : PasswordlessAuth {
  private val json = Json { ignoreUnknownKeys = true }
  private val sessionMutex = Mutex()
  private val client = httpClient {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
  }

  override suspend fun signInWithPassword(account: String, password: String) = sessionMutex.withLock {
    val reply = post("password/login", fields("account" to account, "password" to password))
    installSession(json.decodeFromString(reply))
  }

  override suspend fun signInWithPasskey(provider: PasskeyProvider): Boolean {
    val options = json.decodeFromString<PasskeyOptions>(post("passkeys/authentication/options", fields()))
    val credential = provider.get(options.publicKey.toString()).credentialOrNull() ?: return false
    sessionMutex.withLock {
      installSession(json.decodeFromString(post("passkeys/authentication/verify", options.answer(credential))))
    }
    return true
  }

  suspend fun addPasskey(provider: PasskeyProvider): Boolean {
    val accessToken = requireNotNull(users.getTokens()).accessToken
    val options = json.decodeFromString<PasskeyOptions>(
      post("passkeys/registration/options", fields(), accessToken),
    )
    val credential = provider.create(options.publicKey.toString()).credentialOrNull() ?: return false
    post("passkeys/registration/verify", options.answer(credential), accessToken)
    return true
  }

  suspend fun listPasskeys(): List<RegisteredPasskey> = json.decodeFromString<PasskeyList>(
    request(HttpMethod.Get, "passkeys", accessToken = requireNotNull(users.getTokens()).accessToken),
  ).passkeys

  suspend fun revokePasskey(id: String) {
    request(
      HttpMethod.Delete, "passkeys/${id.encodeURLPathPart()}",
      accessToken = requireNotNull(users.getTokens()).accessToken,
    )
  }

  suspend fun refresh(accountId: String) = sessionMutex.withLock {
    val user = users.getUser() ?: return@withLock
    if (user.id != accountId || !sessionMatchesAuthOrigin(user, endpoint)) return@withLock
    val refreshToken = users.getTokens()?.refreshToken?.takeIf { it.isNotBlank() } ?: return@withLock
    val reply = try {
      json.decodeFromString<SessionResponse>(post("session/refresh", fields("refreshToken" to refreshToken)))
    } catch (failure: AuthHttpException) {
      if (failure.status == 401 || failure.status == 403) users.clear() else throw failure
      return@withLock
    }
    check(reply.user.id == accountId)
    users.replaceTokens(accountId, refreshToken, reply.tokens())
  }

  suspend fun logout(): Boolean = sessionMutex.withLock {
    val refreshToken = users.getTokens()?.refreshToken.orEmpty()
    users.clear()
    if (refreshToken.isEmpty()) true else try {
      post("session/logout", fields("refreshToken" to refreshToken))
      true
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      false
    }
  }

  private suspend fun installSession(reply: SessionResponse) {
    require(accountIdPattern.matches(reply.user.id) && reply.user.account.isNotBlank())
    users.login(UserModel(id = reply.user.id, nickname = reply.user.account, authOrigin = endpoint), reply.tokens())
  }

  private suspend fun post(path: String, body: JsonObject, accessToken: String? = null): String =
    request(HttpMethod.Post, path, body, accessToken)

  private suspend fun request(
    method: HttpMethod,
    path: String,
    body: JsonObject? = null,
    accessToken: String? = null,
  ): String {
    val response = client.request("$endpoint/v1/auth/$path") {
      this.method = method
      if (accessToken != null) bearerAuth(accessToken)
      if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
      }
    }
    if (!response.status.isSuccess()) throw AuthHttpException(response.status.value)
    return response.bodyAsText()
  }

  private fun PasskeyOptions.answer(credential: String) = buildJsonObject {
    put("requestId", requestId)
    put("credential", json.parseToJsonElement(credential))
  }

  private fun PasskeyResult.credentialOrNull(): String? = when (this) {
    is PasskeyResult.Success -> credentialJson
    PasskeyResult.Cancelled -> null
    PasskeyResult.Unavailable -> throw PasskeyUnavailableException()
  }

  private fun fields(vararg entries: Pair<String, String>) = buildJsonObject {
    entries.forEach { (key, value) -> put(key, value) }
  }

  private companion object {
    val accountIdPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
  }
}

internal class AuthHttpException(val status: Int) : Exception("Authentication request failed ($status)")

@Serializable
internal class RegisteredPasskey(val id: String, val createdAt: String)

@Serializable
private class PasskeyList(val passkeys: List<RegisteredPasskey>)

@Serializable
private class PasskeyOptions(val requestId: String, val publicKey: JsonObject)

@Serializable
private class SessionResponse(val user: Account, val accessToken: String, val refreshToken: String) {
  fun tokens() = AuthTokens(accessToken.also { require(it.isNotBlank()) }, refreshToken.also { require(it.isNotBlank()) })
}

@Serializable
private class Account(val id: String, val account: String)
