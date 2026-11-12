package me.zhangls.login

import me.zhangls.login.domain.AccountError
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
    assertEquals(PasswordError.Emoji, LoginValidator.validatePassword("😀"))
    assertEquals(PasswordError.TooShort, LoginValidator.validatePassword("a1!"))
    listOf("abcdefgh", "12345678", "!!!!!!!!").forEach {
      assertEquals(PasswordError.WeakType, LoginValidator.validatePassword(it), it)
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

  @Test
  fun newAccountFollowsServerAccountNameRules() {
    listOf("abc", "notes_tester", "a.b-c_1", "Notes_Tester").forEach {
      assertNull(LoginValidator.validateNewAccount(it), it)
    }
    listOf("ab", ".abc", "-abc", "_abc", "中文账号", "has space", "a".repeat(65)).forEach {
      assertEquals(AccountError.Invalid, LoginValidator.validateNewAccount(it), it)
    }
    assertEquals(AccountError.Empty, LoginValidator.validateNewAccount("  "))
  }

  @Test
  fun normalizesAccountLikeTheServer() {
    assertEquals("notes_tester", LoginValidator.normalizeAccount("  Notes_Tester  "))
  }

  @Test
  fun passwordLengthBoundaryIsEightCodePoints() {
    assertEquals(PasswordError.TooShort, LoginValidator.validatePassword("abcdef1"))
    assertNull(LoginValidator.validatePassword("abcdefg1"))
  }

  @Test
  fun passwordLengthCountsCodePointsLikeTheServer() {
    // U+20000 占两个 UTF-16 单元但不是 emoji：7 个 + 1 位数字 = 8 个码点，UTF-16 长度 15。
    val eightCodePoints = "𠀀".repeat(7) + "1"
    assertNull(LoginValidator.validatePassword(eightCodePoints))
    assertEquals(PasswordError.TooShort, LoginValidator.validatePassword("𠀀".repeat(6) + "1"))
  }

  @Test
  fun rejectsEmojiEvenWhenTheRestIsStrong() {
    listOf(
      "😀",
      "abcdefg1😀",
      "😀😀😀😀😀😀😀😀",
      "👍🏽abcdefg1", // 肤色修饰符
      "👨‍👩‍👧abcdefg1", // 零宽连接符组合
      "❤️abcdefg1", // 变体选择符
      "1️⃣abcdefg1", // 键帽组合符
      "⭐abcdefg1", // 非代理对的表情符号
    ).forEach {
      assertEquals(PasswordError.Emoji, LoginValidator.validatePassword(it), it)
    }
  }

  @Test
  fun allowsNonEmojiSymbols() {
    listOf("abcdefg1!", "p@ssw0rd#", "abcdef1€").forEach {
      assertNull(LoginValidator.validatePassword(it), it)
    }
  }
}
