package me.zhangls.entry

import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
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
import me.zhangls.framework.AppInfo
import me.zhangls.login.api.AccountRegistration
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult
import me.zhangls.login.api.PasskeyUnavailableException
import me.zhangls.login.api.PasswordlessAuth
import me.zhangls.network.httpClient
import me.zhangls.profile.api.AccountAvatar
import me.zhangls.profile.api.AccountProfile

private const val PRODUCTION_AUTH_ORIGIN = "https://auth.notes.zhangls.me"

internal fun configuredAuthEndpoint(appInfo: AppInfo): String? =
  if (appInfo.getUpdateChannel() != null) PRODUCTION_AUTH_ORIGIN else passwordlessEndpoint(BuildKonfig.AUTH_BASE_URL)

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

internal class PasswordlessAuthClient(private val endpoint: String, private val users: UserRepository) : PasswordlessAuth, AccountRegistration {
  private val json = Json { ignoreUnknownKeys = true }
  private val sessionMutex = Mutex()
  private val client = httpClient {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
  }

  override suspend fun sendSignupCode(email: String) {
    post("signup/code", fields("email" to email))
  }

  override suspend fun verifySignupCode(email: String, code: String): String =
    json.decodeFromString<SignupProof>(post("signup/verify", fields("email" to email, "code" to code))).proof

  override suspend fun registerAccount(email: String, proof: String, username: String, password: String) =
    sessionMutex.withLock {
      val reply = post("signup", fields("email" to email, "proof" to proof, "account" to username, "password" to password))
      installSession(json.decodeFromString<SessionResponse>(reply))
    }

  suspend fun updateUsername(userId: String, username: String) {
    withProfileSession(userId) { access ->
      request(HttpMethod.Put, "me/username", fields("account" to username), access)
    }
  }

  suspend fun sendEmailChangeCode(userId: String, email: String, password: String) {
    withProfileSession(userId) { access ->
      post("me/email/code", fields("email" to email, "password" to password), access)
    }
  }

  suspend fun changeEmail(userId: String, email: String, code: String) {
    withProfileSession(userId) { access ->
      request(HttpMethod.Put, "me/email", fields("email" to email, "code" to code), access)
    }
  }

  override suspend fun signInWithPassword(
    account: String,
    password: String,
    onPasswordVerified: (() -> Unit)?,
  ) = sessionMutex.withLock {
    val reply = post("password/login", fields("account" to account, "password" to password))
    installSession(json.decodeFromString(reply), onPasswordVerified)
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

  suspend fun getProfile(userId: String): AccountProfile = withProfileSession(userId) { access ->
    val reply = json.decodeFromString<ProfileReply>(request(HttpMethod.Get, "me", accessToken = access))
    require(reply.id == userId && reply.account.isNotBlank())
    require(reply.avatarRevision == null || accountIdPattern.matches(reply.avatarRevision))
    AccountProfile(reply.id, reply.account, reply.avatarRevision, reply.email)
  }

  suspend fun getAvatar(userId: String): AccountAvatar = withProfileSession(userId) { access ->
    avatarRequest(HttpMethod.Get, access)
  }

  suspend fun updateAvatar(userId: String, jpeg: ByteArray): AccountAvatar = withProfileSession(userId) { access ->
    avatarRequest(HttpMethod.Put, access, jpeg)
  }

  private suspend fun avatarRequest(method: HttpMethod, access: String, jpeg: ByteArray? = null): AccountAvatar {
    val response = client.request("$endpoint/v1/auth/me/avatar") {
      this.method = method
      bearerAuth(access)
      if (jpeg != null) {
        contentType(ContentType.Image.JPEG)
        setBody(jpeg)
      }
    }
    if (!response.status.isSuccess()) throw AuthHttpException(response.status.value)
    require(response.headers[HttpHeaders.ContentType] == ContentType.Image.JPEG.toString())
    val tag = requireNotNull(response.headers[HttpHeaders.ETag])
    require(tag.startsWith('"') && tag.endsWith('"'))
    val revision = tag.removeSurrounding("\"")
    require(accountIdPattern.matches(revision))
    return AccountAvatar(revision, response.body<ByteArray>())
  }

  private suspend fun <T> withProfileSession(userId: String, call: suspend (String) -> T): T =
    sessionMutex.withLock {
      try {
        call(profileAccessToken(userId))
      } catch (failure: AuthHttpException) {
        if (failure.status != 401) throw failure
        refreshLocked(userId)
        call(profileAccessToken(userId))
      }
    }

  private suspend fun profileAccessToken(userId: String): String {
    val user = checkNotNull(users.getUser())
    check(user.id == userId && sessionMatchesAuthOrigin(user, endpoint))
    return checkNotNull(users.getTokens()).accessToken
  }

  suspend fun refresh(accountId: String) = sessionMutex.withLock { refreshLocked(accountId) }

  private suspend fun refreshLocked(accountId: String) {
    val user = users.getUser() ?: return
    if (user.id != accountId || !sessionMatchesAuthOrigin(user, endpoint)) return
    val refreshToken = users.getTokens()?.refreshToken?.takeIf { it.isNotBlank() } ?: return
    val reply = try {
      json.decodeFromString<SessionResponse>(post("session/refresh", fields("refreshToken" to refreshToken)))
    } catch (failure: AuthHttpException) {
      if (failure.status == 401 || failure.status == 403) users.clear() else throw failure
      return
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

  private suspend fun installSession(reply: SessionResponse, onPasswordVerified: (() -> Unit)? = null) {
    require(accountIdPattern.matches(reply.user.id) && reply.user.account.isNotBlank())
    val tokens = reply.tokens()
    // Publishing the session removes the login form, so commit Autofill while its fields still exist.
    onPasswordVerified?.invoke()
    users.login(UserModel(id = reply.user.id, nickname = reply.user.account, authOrigin = endpoint), tokens)
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

@Serializable
private class ProfileReply(
  val id: String,
  val account: String,
  val avatarRevision: String?,
  val email: String? = null,
)

@Serializable
private class SignupProof(val proof: String)
