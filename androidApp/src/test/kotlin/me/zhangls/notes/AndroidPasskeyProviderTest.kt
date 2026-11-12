package me.zhangls.notes

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import me.zhangls.login.api.PasskeyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidPasskeyProviderTest {
  @Test fun reportsNoCreationOption() = runTest {
    val error = runCatching {
      createAndroidPasskey { throw CreateCredentialNoCreateOptionException() }
    }.exceptionOrNull()
    assertTrue(error is PasskeyCreationUnavailableException)
  }

  @Test fun stopsWaitingAndCancelsTheRequestWhenTheProviderDoesNotRespond() = runTest {
    var cancelled = false
    val error = runCatching {
      createAndroidPasskey {
        try {
          awaitCancellation()
        } finally {
          cancelled = true
        }
      }
    }.exceptionOrNull()

    assertEquals(120_000L, testScheduler.currentTime)
    assertTrue(cancelled)
    assertTrue(error is PasskeyCreationTimedOutException)
  }

  @Test fun preservesUserCancellation() = runTest {
    val result = createAndroidPasskey { throw CreateCredentialCancellationException() }
    assertEquals(PasskeyResult.Cancelled, result)
  }
}
