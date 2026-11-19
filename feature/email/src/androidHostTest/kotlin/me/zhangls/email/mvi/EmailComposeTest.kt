package me.zhangls.email.mvi

import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailComposeTest {
  private val me = person(1, true)
  private val alex = person(2)
  private val sam = person(3)
  private val email = EmailModel(
    id = 42,
    sender = alex,
    recipients = listOf(me, sam, sam),
    subject = "Plans",
    body = "Original body",
    createdAt = "2026-11-19T00:00:00Z",
  )

  @Test
  fun replyAllRemovesSelfAndDuplicateRecipientsAndRetainsSource() {
    val draft = prepareEmailDraft(email, "replyAll")
    assertEquals(setOf(2L, 3L), draft.recipients)
    assertEquals(42L, draft.sourceId)
    assertEquals("Re: Plans", draft.subject)
    assertEquals("", draft.body)
    assertEquals(setOf(2L), prepareEmailDraft(email, "reply").recipients)
  }

  @Test
  fun forwardingRequiresNewRecipientAndQuotesOriginalBody() {
    val draft = prepareEmailDraft(email, "forward")
    assertTrue(draft.recipients.isEmpty())
    assertTrue(draft.body.contains("Original body"))
    assertEquals("Fwd: Plans", draft.subject)
    assertEquals("forward", draft.mode)
    val prepared = EmailReducer.reduce(EmailState(), draft)
    assertTrue(prepared.isEmailDraftVisible)
    assertEquals(42L, prepared.draftSourceId)
    assertEquals("new", EmailReducer.reduce(prepared, EmailAction.ClearDraft).draftMode)
  }

  private fun person(id: Long, isDefault: Boolean = false) = AccountModel(
    id = id,
    firstName = "Person",
    lastName = "$id",
    email = "person$id@example.invalid",
    altEmail = "",
    avatar = "avatar_0",
    isDefault = isDefault,
  )
}
