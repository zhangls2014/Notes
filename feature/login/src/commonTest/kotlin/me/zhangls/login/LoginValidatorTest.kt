package me.zhangls.login

import me.zhangls.login.domain.LoginValidator
import me.zhangls.login.domain.PasswordError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginValidatorTest {
  @Test
  fun passwordErrorsKeepTheirPriority() {
    assertEquals(PasswordError.Empty, LoginValidator.validatePassword(""))
    assertEquals(PasswordError.TooShort, LoginValidator.validatePassword("a1!"))
    listOf("abcdefgh", "12345678", "!!!!!!!!").forEach {
      assertEquals(PasswordError.WeakType, LoginValidator.validatePassword(it))
    }
  }

  @Test
  fun acceptsAnyTwoCharacterTypesAtTheMinimumLength() {
    listOf("abcdefg1", "abcdefg!", "1234567!", "中文中文中文中1", "abcdefg ").forEach {
      assertNull(LoginValidator.validatePassword(it), it)
    }
  }

  @Test
  fun requiresBothAValidAccountAndPassword() {
    assertFalse(LoginValidator.validateAll(" ", "abcdefg1"))
    assertFalse(LoginValidator.validateAll("account", "abcdefgh"))
    assertTrue(LoginValidator.validateAll("account", "abcdefg1"))
  }
}
