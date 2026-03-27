package me.zhangls.login.domain

/**
 * 登录输入验证
 */
object LoginValidator {
  private const val MIN_PASSWORD_LENGTH = 8

  fun validateAccount(account: String): AccountError? {
    return if (account.isBlank()) {
      AccountError.Empty
    } else {
      null
    }
  }

  /**
   * 密码规则：密码至少包含字母、数字、特殊符号中的两种
   */
  fun validatePassword(password: String): PasswordError? {
    if (password.isEmpty()) return PasswordError.Empty
    if (password.length < MIN_PASSWORD_LENGTH) return PasswordError.TooShort

    // 字母
    val hasLetter = password.any { it.isLetter() }
    // 数字
    val hasDigit = password.any { it.isDigit() }
    // 特殊符号
    val hasSpecial = password.any { it.isLetterOrDigit().not() }

    val typeCount = listOf(hasLetter, hasDigit, hasSpecial).count { it }

    if (typeCount < 2) {
      return PasswordError.WeakType
    }

    return null
  }

  fun validateAll(account: String, password: String): Boolean {
    validateAccount(account)?.let { return false }
    validatePassword(password)?.let { return false }

    return true
  }
}