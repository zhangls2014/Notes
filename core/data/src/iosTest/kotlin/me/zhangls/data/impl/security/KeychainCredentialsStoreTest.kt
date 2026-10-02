package me.zhangls.data.impl.security

import kotlinx.coroutines.test.runTest
import me.zhangls.data.model.AuthTokens
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeychainCredentialsStoreTest {
  // Gradle runs a standalone Kotlin/Native executable: observed errSecNotAvailable (-25291).
  // Enable in an app-hosted iOS test harness with a Keychain-capable application identity.
  @Ignore
  @Test
  fun keychainRoundTripUpdateAndDeletion() = runTest {
    // Separate service: never touch an installed app's actual session.
    val service = "me.zhangls.notes.test.${kotlin.random.Random.nextLong()}"
    val store = KeychainCredentialsStore(service)
    try {
      assertNull(store.read())
      store.write(StoredCredentials("one", AuthTokens("access", "refresh")))
      assertEquals("access", KeychainCredentialsStore(service).read()?.tokens?.accessToken)
      assertEquals("refresh", store.read()?.tokens?.refreshToken)
      store.write(StoredCredentials("two", AuthTokens("rotated")))
      assertEquals("two", store.read()?.userId)
      assertEquals("rotated", store.read()?.tokens?.accessToken)
      store.clear()
      assertNull(store.read())
      store.clear()
    } finally { store.clear() }
  }
}
