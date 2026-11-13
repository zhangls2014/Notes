package me.zhangls.entry

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.login.api.AccountRegistration
import me.zhangls.login.api.EmailAlreadyRegisteredException
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult
import me.zhangls.login.api.PasskeyUnavailableException
import me.zhangls.login.api.PasswordlessAuth
import me.zhangls.profile.api.AccountAvatar
import me.zhangls.profile.api.AccountProfile
import me.zhangls.profile.api.RegisteredPasskey

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

internal class PasswordlessAuthClient(
  private val endpoint: String,
  private val users: UserRepository,
  private val client: HttpClient,
  private val reportFailure: (String) -> Unit = ::logAuthFailure,
) : PasswordlessAuth, AccountRegistration {
  private val json = Json { ignoreUnknownKeys = true }
  private val session = AccountSession(endpoint, users) { accountId, refreshToken, operationId ->
    val body = post("session/refresh", fields("refreshToken" to refreshToken, "operationId" to operationId))
    val reply = decode<SessionResponse>(body)
    if (reply.user.id != accountId || reply.accessToken.isBlank() || reply.refreshToken.isBlank()) {
      invalidResponse(body.requestId)
    }
    reply.tokens()
  }

  override suspend fun sendSignupCode(email: String) {
    try {
      post("signup/code", fields("email" to email))
    } catch (failure: AuthHttpException) {
      if (failure.status == 409 && failure.code == "email_already_registered") throw EmailAlreadyRegisteredException()
      throw failure
    }
  }

  override suspend fun verifySignupCode(email: String, code: String): String =
    decode<SignupProof>(post("signup/verify", fields("email" to email, "code" to code))).proof

  override suspend fun registerAccount(email: String, proof: String, username: String, password: String) =
    session.establish {
      val reply = post("signup", fields("email" to email, "proof" to proof, "account" to username, "password" to password))
      installSession(decode<SessionResponse>(reply))
    }

  suspend fun updateUsername(userId: String, username: String) {
    session.request(userId) { access ->
      request(HttpMethod.Put, "me/username", fields("account" to username), access)
    }
  }

  suspend fun verifyEmailChangePassword(userId: String, password: String): Boolean {
    return try {
      session.request(userId) { access ->
        post("me/email/password", fields("password" to password), access)
      }
      true
    } catch (failure: AuthHttpException) {
      if (failure.status != 403 || failure.code != "invalid_credentials") throw failure
      false
    }
  }

  suspend fun sendEmailChangeCode(userId: String, email: String, password: String) {
    session.request(userId) { access ->
      post("me/email/code", fields("email" to email, "password" to password), access)
    }
  }

  suspend fun changeEmail(userId: String, email: String, code: String) {
    session.request(userId) { access ->
      request(HttpMethod.Put, "me/email", fields("email" to email, "code" to code), access)
    }
  }

  override suspend fun signInWithPassword(
    email: String,
    password: String,
    onPasswordVerified: (() -> Unit)?,
  ) = session.establish {
    val reply = post("password/login", fields("account" to email, "password" to password))
    installSession(decode(reply), onPasswordVerified)
  }

  override suspend fun signInWithPasskey(email: String, provider: PasskeyProvider): Boolean {
    val options = decode<PasskeyOptions>(post("passkeys/authentication/options", fields("email" to email)))
    val credential = provider.get(options.publicKey.toString()).credentialOrNull() ?: return false
    session.establish {
      installSession(decode(post("passkeys/authentication/verify", options.answer(credential))))
    }
    return true
  }

  suspend fun addPasskey(provider: PasskeyProvider): Boolean {
    val user = users.getUser() ?: throw AuthHttpException(401, "invalid_session")
    val current = session.snapshot(user.id)
    val options = session.request(current) { access ->
      decode<PasskeyOptions>(post("passkeys/registration/options", fields(), access))
    }
    val credential = provider.create(options.publicKey.toString()).credentialOrNull() ?: return false
    val brand = provider.deviceBrand?.trim()?.takeIf {
      it.length in 1..64 && it.first().isLetterOrDigit() && it.all { char -> char.isLetterOrDigit() || char in " ._-" } &&
        !it.equals("unknown", ignoreCase = true)
    }
    session.request(current) { access ->
      post("passkeys/registration/verify", options.answer(credential, brand), access)
    }
    return true
  }

  suspend fun listPasskeys(): List<RegisteredPasskey> {
    val user = users.getUser() ?: throw AuthHttpException(401, "invalid_session")
    return session.request(user.id) { access ->
      decode<PasskeyList>(request(HttpMethod.Get, "passkeys", accessToken = access)).passkeys.map {
        RegisteredPasskey(it.id, it.createdAt, it.provider, it.deviceBrand)
      }
    }
  }

  suspend fun revokePasskey(id: String) {
    val user = users.getUser() ?: throw AuthHttpException(401, "invalid_session")
    session.request(user.id) { access ->
      request(HttpMethod.Delete, "passkeys/${id.encodeURLPathPart()}", accessToken = access)
    }
  }

  suspend fun getProfile(userId: String): AccountProfile = session.request(userId) { access ->
    val body = request(HttpMethod.Get, "me", accessToken = access)
    val reply = decode<ProfileReply>(body)
    if (reply.id != userId || reply.account.isBlank() ||
      (reply.avatarRevision != null && !accountIdPattern.matches(reply.avatarRevision))
    ) invalidResponse(body.requestId)
    AccountProfile(reply.id, reply.account, reply.avatarRevision, reply.email)
  }

  suspend fun getAvatar(userId: String): AccountAvatar = session.request(userId) { access ->
    avatarRequest(HttpMethod.Get, access)
  }

  suspend fun updateAvatar(userId: String, jpeg: ByteArray): AccountAvatar = session.request(userId) { access ->
    avatarRequest(HttpMethod.Put, access, jpeg)
  }

  private suspend fun avatarRequest(method: HttpMethod, access: String, jpeg: ByteArray? = null): AccountAvatar {
    return exchange {
      val response = client.request("$endpoint/v1/auth/me/avatar") {
        this.method = method
        bearerAuth(access)
        if (jpeg != null) {
          contentType(ContentType.Image.JPEG)
          setBody(jpeg)
        }
      }
      response.requireSuccess()
      val tag = response.headers[HttpHeaders.ETag]
      val revision = tag?.removeSurrounding("\"")
      if (response.headers[HttpHeaders.ContentType] != ContentType.Image.JPEG.toString() ||
        tag == revision || revision == null || !accountIdPattern.matches(revision)
      ) invalidResponse(response.requestId())
      AccountAvatar(revision, response.body<ByteArray>())
    }
  }

  suspend fun logout(): Boolean {
    val credentials = session.clear() ?: return true
    if (credentials.refreshToken.isEmpty()) return true
    return try {
      post("session/logout", buildJsonObject {
        put("refreshToken", credentials.refreshToken)
        credentials.refreshOperationId?.let { put("operationId", it) }
      })
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

  private suspend fun post(path: String, body: JsonObject, accessToken: String? = null): AuthBody =
    request(HttpMethod.Post, path, body, accessToken)

  private suspend fun request(
    method: HttpMethod,
    path: String,
    body: JsonObject? = null,
    accessToken: String? = null,
  ): AuthBody = exchange {
    val response = client.request("$endpoint/v1/auth/$path") {
      this.method = method
      if (accessToken != null) bearerAuth(accessToken)
      if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body.toString())
      }
    }
    response.requireSuccess()
    AuthBody(response.bodyAsText(), response.requestId())
  }

  private suspend fun HttpResponse.requireSuccess() {
    if (status.isSuccess()) return
    val code = try {
      json.decodeFromString<AuthError>(bodyAsText()).error.takeIf(errorCodePattern::matches)
    } catch (_: SerializationException) {
      null
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: IOException) {
      null
    }
    throw AuthHttpException(status.value, code, requestId())
  }

  private suspend fun <T> exchange(call: suspend () -> T): T = try {
    call()
  } catch (failure: AuthHttpException) {
    reportFailure("HTTP status=${failure.status} code=${failure.code} requestId=${failure.requestId}")
    throw failure
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (failure: IOException) {
    val kind = when (failure) {
      is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> "timeout"
      else -> "connection"
    }
    reportFailure("Transport failure: $kind")
    throw failure
  }

  private inline fun <reified T> decode(body: AuthBody): T = try {
    json.decodeFromString(body.text)
  } catch (_: SerializationException) {
    invalidResponse(body.requestId)
  }

  private fun invalidResponse(requestId: String?): Nothing {
    reportFailure("Invalid response requestId=$requestId")
    throw AuthResponseException(requestId)
  }

  private fun HttpResponse.requestId(): String? = headers["X-Request-Id"]?.takeIf(accountIdPattern::matches)

  private fun PasskeyOptions.answer(credential: String, deviceBrand: String? = null) = buildJsonObject {
    put("requestId", requestId)
    put("credential", json.parseToJsonElement(credential))
    deviceBrand?.let { put("deviceBrand", it) }
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
    val errorCodePattern = Regex("[a-z][a-z0-9_]{0,63}")
    val accountIdPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
  }
}

@Serializable
private class AuthError(val error: String)

internal expect fun logAuthFailure(message: String)

private class AuthBody(val text: String, val requestId: String?)

internal class AuthResponseException(val requestId: String?) : Exception("Invalid authentication response")

internal class AuthHttpException(val status: Int, val code: String? = null, val requestId: String? = null) :
  Exception("Authentication request failed ($status)") {
  val invalidSession: Boolean get() = status == 401 && code == "invalid_session"
}

@Serializable
private class PasskeyResponse(
  val id: String,
  val createdAt: String,
  val provider: String? = null,
  val deviceBrand: String? = null,
)

@Serializable
private class PasskeyList(val passkeys: List<PasskeyResponse>)

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
