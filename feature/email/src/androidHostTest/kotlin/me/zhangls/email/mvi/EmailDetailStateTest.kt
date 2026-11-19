package me.zhangls.email.mvi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmailDetailStateTest {
  @Test
  fun loadingIsDistinctFromFailureAndMissingMail() = runBlocking {
    assertEquals(
      listOf(EmailDetailState.Loading, EmailDetailState.NotFound),
      flowOf<EmailModel?>(null).asEmailDetailState().toList(),
    )
    assertEquals(
      listOf(EmailDetailState.Loading, EmailDetailState.Failed),
      flow<EmailModel?> { error("Connection failed") }.asEmailDetailState().toList(),
    )
  }

  @Test
  fun loadedMailIsPreservedAndCancellationIsNotAnError(): Unit = runBlocking {
    val email = EmailModel(
      id = 1,
      sender = AccountModel(1, "Ada", "", "ada@example.invalid", "", "avatar_1"),
      subject = "Plans",
      body = "Hello",
      createdAt = "09:30",
    )
    assertEquals(
      listOf(EmailDetailState.Loading, EmailDetailState.Loaded(email)),
      flowOf(email).asEmailDetailState().toList(),
    )
    assertThrows(CancellationException::class.java) {
      runBlocking {
        flow<EmailModel?> { throw CancellationException() }.asEmailDetailState().toList()
      }
    }
  }
}
