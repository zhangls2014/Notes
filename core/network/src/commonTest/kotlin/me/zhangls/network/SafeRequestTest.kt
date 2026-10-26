package me.zhangls.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class SafeRequestTest {
  @Test
  fun cancellationEscapesTheNetworkBoundary() = runBlocking {
    val cancellation = CancellationException("Request cancelled")
    val thrown = assertFailsWith<CancellationException> {
      safeRequest<Unit> { throw cancellation }
    }
    assertSame(cancellation, thrown)
  }

  @Test
  fun ordinaryFailurePreservesItsCause() = runBlocking {
    val cause = IllegalStateException("Transport unavailable")
    val result = safeRequest<Unit> { throw cause }
    val failure = assertIs<NetworkResult.Failure>(result)
    assertSame(cause, assertIs<NetworkError.Unknown>(failure.error).throwable)
  }
}
