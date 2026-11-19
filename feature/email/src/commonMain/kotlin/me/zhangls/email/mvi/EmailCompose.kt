package me.zhangls.email.mvi

import me.zhangls.data.model.EmailModel

internal fun prepareEmailDraft(email: EmailModel, mode: String): EmailAction.PrepareDraft {
  val recipients = when (mode) {
    "forward" -> emptySet()

    "replyAll" -> (email.recipients + email.sender).filterNot { it.isDefault }.map { it.id }.toSet()

    else -> if (email.sender.isDefault) {
      email.recipients.filterNot { it.isDefault }.map { it.id }.toSet()
    } else {
      setOf(email.sender.id)
    }
  }
  val prefix = if (mode == "forward") "Fwd: " else "Re: "
  val quoted = if (mode == "forward") {
    "\n\n----------\n${email.sender.fullName} <${email.sender.email}>\n" +
      "${email.createdAt}\n${email.subject}\n\n${email.body}"
  } else {
    ""
  }
  return EmailAction.PrepareDraft(
    recipients = recipients,
    subject = if (email.subject.startsWith(prefix, ignoreCase = true)) email.subject else prefix + email.subject,
    body = quoted,
    mode = mode,
    sourceId = email.id,
  )
}
