package me.zhangls.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.zhangls.data.impl.security.SecureTokenStore
import me.zhangls.data.impl.security.StoredCredentials
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import org.koin.core.annotation.Singleton

@Singleton(binds = [UserRepository::class])
internal class UserRepositoryImpl(
  private val prefsDataStore: DataStore<Preferences>,
  private val secureStore: SecureTokenStore,
) : UserRepository {
  private val mutex = Mutex()
  private val userKey = stringPreferencesKey("user")
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  override val userFlow: Flow<UserModel?> = prefsDataStore.data
    .onStart { mutex.withLock { migrate() } }
    .map {
      mutex.withLock {
        // Read the latest snapshot: a login/logout may have occurred while this emission waited.
        val user = readProfile()
        user?.takeIf { matchingCredentials(it) != null }
      }
    }
    .retryWhen { cause, attempt ->
      // Locked Keychain / transient I/O must not kill root UI observation or erase a session.
      // Request-level getTokens still propagates errors so no request uses stale credentials.
      if (cause is CancellationException || cause !is Exception) return@retryWhen false
      delay(minOf(attempt + 1, 30) * 1_000)
      true
    }
    .distinctUntilChanged()

  override suspend fun getUser(): UserModel? = mutex.withLock {
    migrate()
    readProfile()?.takeIf { matchingCredentials(it) != null }
  }

  override suspend fun getTokens(): AuthTokens? = mutex.withLock {
    migrate()
    val user = readProfile() ?: return@withLock null
    matchingCredentials(user)?.tokens
  }

  override suspend fun login(user: UserModel, tokens: AuthTokens): Unit = mutex.withLock {
    require(tokens.accessToken.isNotBlank())
    secureStore.write(StoredCredentials(user.id, tokens))
    prefsDataStore.edit { it[userKey] = json.encodeToString(user) }
  }

  override suspend fun updateAvatar(avatar: String) = updateProfile { it.copy(avatar = avatar) }

  override suspend fun updateEmailSearchHistory(keyword: String) {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return
    updateProfile { user ->
      user.copy(emailSearchHistory = (listOf(normalized) + user.emailSearchHistory.filter { it != normalized }).take(10))
    }
  }

  override suspend fun deleteEmailSearchHistory(keyword: String) {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return
    updateProfile { user -> user.copy(emailSearchHistory = user.emailSearchHistory.filter { it != normalized }) }
  }

  override suspend fun clear(): Unit = mutex.withLock {
    // Scrub BEFORE removing secure credentials: a failed final deletion must never allow
    // the legacy migration path to recreate a session that was already cleared.
    prefsDataStore.edit { prefs ->
      decode(prefs[userKey])?.let { prefs[userKey] = json.encodeToString(it) }
    }
    secureStore.clear()
    prefsDataStore.edit { it.remove(userKey) }
  }

  private suspend fun updateProfile(transform: (UserModel) -> UserModel): Unit = mutex.withLock {
    migrate()
    prefsDataStore.edit { prefs ->
      decode(prefs[userKey])?.let { prefs[userKey] = json.encodeToString(transform(it)) }
    }
  }

  private suspend fun matchingCredentials(user: UserModel): StoredCredentials? {
    val credentials = secureStore.read()
    if (credentials?.userId == user.id) return credentials
    // Emit a DataStore change so existing UI collectors also observe an invalidated session.
    prefsDataStore.edit { it.remove(userKey) }
    return null
  }

  private suspend fun readProfile(): UserModel? = decode(prefsDataStore.data.first()[userKey])

  private fun decode(raw: String?): UserModel? = raw?.let {
    runCatching { json.decodeFromString<UserModel>(it) }.getOrNull()
  }

  /** Write and verify before removing legacy plaintext; failure leaves the original retryable. */
  private suspend fun migrate() {
    val raw = prefsDataStore.data.first()[userKey] ?: return
    val user = decode(raw) ?: return
    val obj = json.parseToJsonElement(raw).jsonObject
    if ("accessToken" !in obj && "refreshToken" !in obj) return
    val access = obj["accessToken"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (access.isNotBlank()) {
      val refresh = obj["refreshToken"]?.jsonPrimitive?.contentOrNull.orEmpty()
      // Prefer an already migrated same-account credential after an interrupted migration.
      val existing = secureStore.read()
      if (existing?.userId != user.id) secureStore.write(StoredCredentials(user.id, AuthTokens(access, refresh)))
      val saved = secureStore.read()
      check(saved?.userId == user.id && saved.tokens.accessToken.isNotBlank()) { "Credential migration verification failed" }
    }
    prefsDataStore.edit { it[userKey] = json.encodeToString(user) }
  }
}
