package me.zhangls.login.domain

/**
 * 登录与注册的输入验证。
 *
 * 规则以服务端为准（`server/auth/src/security.ts`），本地只做前置拦截：
 * - 账号名：`^[a-z0-9][a-z0-9._-]{2,63}$`，输入先 trim 并转为小写，与服务端
 *   `normalizeUsername` 一致。
 * - 密码：至少 8 个字符（按码点计数），且不得包含表情符号，与服务端
 *   `readNewPassword` 一致；登录与注册使用同一条长度规则。
 * - 字符类型：至少包含字母、数字、特殊符号中的两种。服务端只校验长度与 emoji，
 *   这一条是客户端附加要求，注册与登录共用，避免注册通过后登录被判弱密码。
 */
object LoginValidator {
  private const val MIN_PASSWORD_LENGTH = 8
  private val accountNamePattern = Regex("[a-z0-9][a-z0-9._-]{2,63}")

  /**
   * 表情符号及用于组合表情的区段。表情常由多个码点拼成（肤色修饰、零宽连接、
   * 变体选择符、旗帜标签），因此这些组件字符本身也一并拒绝。
   */
  private val emojiRanges = listOf(
    0x1F000..0x1FAFF, // 麻将、扑克、表情、交通、补充符号
    0x2600..0x27BF, // 杂项符号、装饰符号（✅ ❤ ✨ 等）
    0x2B00..0x2BFF, // 星形与箭头符号（⭐ 等）
    0xFE0F..0xFE0F, // 变体选择符，决定签名是否渲染为彩色表情
    0x200D..0x200D, // 零宽连接符，用于组合表情序列
    0x20E3..0x20E3, // 键帽组合符
    0xE0020..0xE007F, // 标签字符，用于旗帜序列
  )

  /** 与服务端相同的规范化：去掉首尾空白并统一小写。 */
  fun normalizeAccount(account: String): String = account.trim().lowercase()

  fun validateAccount(account: String): AccountError? = if (account.isBlank()) {
    AccountError.Empty
  } else {
    null
  }

  /** 注册账号名：比登录更严，与服务端 `normalizeUsername` 的校验范围一致。 */
  fun validateNewAccount(account: String): AccountError? = when {
    account.isBlank() -> AccountError.Empty
    !accountNamePattern.matches(normalizeAccount(account)) -> AccountError.Invalid
    else -> null
  }

  /** 密码规则：至少 8 个字符、不含表情符号，且包含字母、数字、特殊符号中的两种。 */
  fun validatePassword(password: String): PasswordError? = when {
    password.isEmpty() -> PasswordError.Empty
    containsEmoji(password) -> PasswordError.Emoji
    password.codePointLength() < MIN_PASSWORD_LENGTH -> PasswordError.TooShort
    characterTypes(password) < 2 -> PasswordError.WeakType
    else -> null
  }

  fun validateAll(account: String, password: String): Boolean =
    validateAccount(account) == null && validatePassword(password) == null

  private fun characterTypes(password: String): Int = listOf(
    // 字母
    password.any { it.isLetter() },
    // 数字
    password.any { it.isDigit() },
    // 特殊符号
    password.any { it.isLetterOrDigit().not() },
  ).count { it }

  /**
   * 逐个码点检查表情符号区段。代理对合成为一个码点后再判断，避免把 emoji 拆成
   * 两个落在区段外的 UTF-16 单元。
   */
  private fun containsEmoji(text: String): Boolean {
    var index = 0
    while (index < text.length) {
      val current = text[index]
      val codePoint = if (current.isHighSurrogate() &&
        index + 1 < text.length &&
        text[index + 1].isLowSurrogate()
      ) {
        index++
        0x10000 + ((current.code - 0xD800) shl 10) + (text[index].code - 0xDC00)
      } else {
        current.code
      }
      if (emojiRanges.any { codePoint in it }) return true
      index++
    }
    return false
  }

  /** 按码点计数，与服务端 `[...password].length` 保持一致，代理对不重复计入。 */
  private fun String.codePointLength(): Int = count { it.isLowSurrogate().not() }
}
