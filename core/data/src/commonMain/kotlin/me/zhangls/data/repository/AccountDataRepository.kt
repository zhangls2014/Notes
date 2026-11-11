package me.zhangls.data.repository

import me.zhangls.data.model.EmailModel

/** Opens the local mail store before authenticated UI is mounted. */
interface AccountDataRepository {
  /** Repeated opens of the active account keep its current store and repository instances. */
  suspend fun open(accountId: String, initialEmails: List<EmailModel>)

  /** Retires the active store so its existing repository instances cannot be reused. */
  suspend fun close()
}
