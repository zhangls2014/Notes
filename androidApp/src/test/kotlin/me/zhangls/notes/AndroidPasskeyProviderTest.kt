package me.zhangls.notes

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import me.zhangls.login.api.PasskeyAlreadyExistsException
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import me.zhangls.login.api.PasskeyLoginTimedOutException
import me.zhangls.login.api.PasskeyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidPasskeyProviderTest {
  @Test fun reportsAnExistingPasskeyOnlyForInvalidState() = runTest {
    val error = runCatching {
      createAndroidPasskey { throw CreatePublicKeyCredentialDomException(InvalidStateError()) }
    }.exceptionOrNull()
    assertTrue(error is PasskeyAlreadyExistsException)
    assertEquals(PasskeyResult.Unavailable, createAndroidPasskey {
      throw CreatePublicKeyCredentialDomException(NotAllowedError())
    })
  }

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

  @Test fun loginCancelsTheRequestWhenTheProviderDoesNotRespond() = runTest {
    var cancelled = false
    val error = runCatching {
      getAndroidPasskey {
        try {
          awaitCancellation()
        } finally {
          cancelled = true
        }
      }
    }.exceptionOrNull()

    assertEquals(120_000L, testScheduler.currentTime)
    assertTrue(cancelled)
    assertTrue(error is PasskeyLoginTimedOutException)
  }

  @Test fun loginReportsMissingCredentialsAndPreservesUserCancellation() = runTest {
    assertEquals(PasskeyResult.Unavailable, getAndroidPasskey { throw NoCredentialException() })
    assertEquals(PasskeyResult.Cancelled, getAndroidPasskey { throw GetCredentialCancellationException() })
    assertEquals(0L, testScheduler.currentTime)
  }
}
