package me.zhangls.entry

import me.zhangls.data.model.UserModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasswordlessEndpointTest {
  @Test
  fun acceptsAnHttpsOriginAndTreatsBlankAsDemoMode() {
    assertNull(passwordlessEndpoint(""))
    assertEquals("https://auth.test.example", passwordlessEndpoint("https://auth.test.example/"))
  }

  @Test
  fun accountScopedLinkMustNameTheActiveAccount() {
    val link = "https://notes.zhangls.me/email?id=42"
    assertFalse(isDeepLinkForAccount(link, "account-a"))
    assertFalse(isDeepLinkForAccount("$link&accountId=account-b", "account-a"))
    assertTrue(isDeepLinkForAccount("$link&accountId=account-a", "account-a"))
  }

  @Test
  fun storedSessionsCannotCrossAuthOrigins() {
    val endpoint = "https://auth.test.example"
    val demo = UserModel("local", "Demo")
    val account = UserModel("account-a", "Account", authOrigin = endpoint)
    assertTrue(sessionMatchesAuthOrigin(demo, null))
    assertFalse(sessionMatchesAuthOrigin(demo, endpoint))
    assertTrue(sessionMatchesAuthOrigin(account, endpoint))
    assertFalse(sessionMatchesAuthOrigin(account, "https://another.example"))
    assertFalse(sessionMatchesAuthOrigin(account, null))
  }

  @Test
  fun rejectsCredentialsPathsFragmentsAndCleartext() {
    for (url in listOf(
      "http://auth.test.example",
      "https://user:secret@auth.test.example",
      "https://auth.test.example/api",
      "https://auth.test.example/#fragment",
      "https://auth.test.example/?key=secret",
    )) {
      assertFailsWith<IllegalArgumentException> { passwordlessEndpoint(url) }
    }
  }
}
