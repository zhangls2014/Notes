package me.zhangls.data.repository

import me.zhangls.data.model.EmailModel

/** Opens the account mail repository before authenticated UI is mounted. */
interface AccountDataRepository {
  /**
   * initialEmails is used only by the legacy Room implementation.
   * Repeated opens of the active account keep its current repository instance.
   */
  suspend fun open(accountId: String, initialEmails: List<EmailModel>)

  /** Retires the active store so its existing repository instances cannot be reused. */
  suspend fun close()
}
