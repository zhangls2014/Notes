package me.zhangls.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.zhangls.data.impl.security.SecureTokenStore
import me.zhangls.data.impl.security.StoredCredentials
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import okio.FileSystem
import kotlin.test.*

class UserRepositorySecurityTest {
  @Test
  fun migratesLegacyCredentialsAndRemovesPlaintext() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore()
    val repository = UserRepositoryImpl(prefs, store)
    try {
      prefs.edit { it[stringPreferencesKey("user")] = """{"id":"one","nickname":"User","accessToken":"old-secret","refreshToken":"refresh-secret"}""" }
      assertEquals("one", repository.userFlow.first()?.id)
      assertEquals("old-secret", repository.getTokens()?.accessToken)
      val raw = prefs.data.first()[stringPreferencesKey("user")]!!
      assertFalse(raw.contains("Token"))
      assertFalse(raw.contains("secret"))
      assertEquals("old-secret", UserRepositoryImpl(prefs, store).getTokens()?.accessToken)
      assertTrue(repository.updateAvatar("avatar", repository.getUser()!!))
      assertEquals("old-secret", repository.getTokens()?.accessToken)
      repository.clear()
      assertNull(store.value)
      assertNull(repository.userFlow.first())
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun avatarUpdateRejectsChangedUserOrAvatarButKeepsSearchHistory() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-avatar-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val repository = UserRepositoryImpl(prefs, FakeStore())
    try {
      val user = UserModel("local", "First user", "old")
      repository.login(user, AuthTokens("secret"))
      repository.updateEmailSearchHistory("query")
      assertTrue(repository.updateAvatar("new", user))
      assertEquals(listOf("query"), repository.getUser()?.emailSearchHistory)
      assertFalse(repository.updateAvatar("stale", user))
      assertEquals("new", repository.getUser()?.avatar)
      repository.login(user.copy(nickname = "Another user"), AuthTokens("other"))
      assertFalse(repository.updateAvatar("wrong-user", user))
      assertEquals("old", repository.getUser()?.avatar)
      repository.clear()
      assertFalse(repository.updateAvatar("signed-out", user))
      assertNull(repository.getUser())
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun failedMigrationRetainsLegacyDataForRetry() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore().apply { failWrites = true }
    val repository = UserRepositoryImpl(prefs, store)
    try {
      prefs.edit { it[stringPreferencesKey("user")] = """{"id":"one","nickname":"User","accessToken":"secret"}""" }
      assertFailsWith<IllegalStateException> { repository.getTokens() }
      assertTrue(prefs.data.first()[stringPreferencesKey("user")]!!.contains("secret"))
      store.failWrites = false
      assertEquals("secret", repository.getTokens()?.accessToken)
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun loginStoresOnlyProfileAndRejectsMismatchedCredentials() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore()
    val repository = UserRepositoryImpl(prefs, store)
    try {
      repository.login(UserModel("one", "User"), AuthTokens("secret", "refresh"))
      assertFalse(prefs.data.first()[stringPreferencesKey("user")]!!.contains("secret"))
      assertEquals("secret", repository.getTokens()?.accessToken)
      store.value = StoredCredentials("other", AuthTokens("other-secret"))
      assertNull(repository.getTokens())
      assertNull(repository.userFlow.first())
      store.value = null
      assertNull(repository.userFlow.first())
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun missingCredentialsInvalidatePersistedSession() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore()
    val repository = UserRepositoryImpl(prefs, store)
    try {
      repository.login(UserModel("one", "User"), AuthTokens("secret"))
      store.value = null // Platform detected permanently lost key or corrupt ciphertext.
      assertNull(repository.getTokens())
      assertNull(prefs.data.first()[stringPreferencesKey("user")])
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun interruptedMigrationKeepsNewerSecureCredentials() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore().apply { value = StoredCredentials("one", AuthTokens("new-secret")) }
    val repository = UserRepositoryImpl(prefs, store)
    try {
      prefs.edit { it[stringPreferencesKey("user")] = """{"id":"one","nickname":"User","accessToken":"old-secret"}""" }
      assertEquals("new-secret", repository.getTokens()?.accessToken)
      assertFalse(prefs.data.first()[stringPreferencesKey("user")]!!.contains("Token"))
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun failedLogoutCannotResurrectLegacyCredentials() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    var failDeletes = true
    val guarded = object : DataStore<Preferences> {
      override val data = prefs.data
      override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        prefs.updateData { old ->
          val next = transform(old)
          check(!failDeletes || next[stringPreferencesKey("user")] != null) { "Delete failed" }
          next
        }
    }
    val store = FakeStore()
    val repository = UserRepositoryImpl(guarded, store)
    try {
      prefs.edit { it[stringPreferencesKey("user")] = """{"id":"one","nickname":"User","accessToken":"old-secret"}""" }
      assertFailsWith<IllegalStateException> { repository.clear() }
      assertNull(store.value)
      failDeletes = false
      assertNull(repository.getTokens())
      assertNull(repository.getUser())
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  @Test
  fun observationRetriesUnavailableStorageWithoutDeletingSession() = runTest {
    val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "notes-token-test-${kotlin.random.Random.nextLong()}.preferences_pb"
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) { path }
    val store = FakeStore()
    val repository = UserRepositoryImpl(prefs, store)
    try {
      repository.login(UserModel("one", "User"), AuthTokens("secret"))
      store.readFailures = 1
      assertEquals("one", repository.userFlow.first()?.id)
      assertEquals("secret", repository.getTokens()?.accessToken)
      assertTrue(prefs.data.first()[stringPreferencesKey("user")] != null)
    } finally { FileSystem.SYSTEM.delete(path, mustExist = false) }
  }

  private class FakeStore : SecureTokenStore {
    var value: StoredCredentials? = null
    var failWrites = false
    var readFailures = 0
    override suspend fun read(): StoredCredentials? {
      if (readFailures > 0) {
        readFailures--
        error("Storage temporarily unavailable")
      }
      return value
    }
    override suspend fun write(credentials: StoredCredentials) {
      check(!failWrites)
      value = credentials
    }
    override suspend fun clear() { value = null }
  }
}
