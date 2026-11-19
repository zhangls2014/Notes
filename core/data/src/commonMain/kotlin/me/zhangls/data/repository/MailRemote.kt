package me.zhangls.data.repository

import kotlinx.serialization.json.JsonObject

/** Authenticated transport supplied by the application composition root. */
interface MailRemote {
  suspend fun request(accountId: String, input: JsonObject): String
}

/** Transport and HTTP failures are translated at the composition boundary. */
class MailRequestException(cause: Throwable, val status: Int? = null) : Exception("Mail request failed", cause)
