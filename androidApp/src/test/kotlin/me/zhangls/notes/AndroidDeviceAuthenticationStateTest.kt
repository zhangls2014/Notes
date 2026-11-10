package me.zhangls.notes

import me.zhangls.entry.DeviceAuthenticationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidDeviceAuthenticationStateTest {
  @Test
  fun cancelledAttemptBlocksNewAttemptUntilItsSystemResultArrives() {
    val state = AndroidDeviceAuthenticationState()
    val first = checkNotNull(state.start(AndroidAuthenticationMethod.Biometric))

    first.cancel()
    assertNull(state.start(AndroidAuthenticationMethod.Biometric))
    state.finish(AndroidAuthenticationMethod.Biometric, DeviceAuthenticationResult.Success)

    assertTrue(first.isCancelled)
    assertNotNull(state.start(AndroidAuthenticationMethod.Biometric))
  }

  @Test
  fun biometricResultCannotFinishCredentialFallback() {
    val state = AndroidDeviceAuthenticationState()
    val attempt = checkNotNull(state.start(AndroidAuthenticationMethod.Biometric))

    assertTrue(state.switchToCredential())
    state.finish(AndroidAuthenticationMethod.Biometric, DeviceAuthenticationResult.Success)

    assertFalse(attempt.isCompleted)
    state.finish(AndroidAuthenticationMethod.Credential, DeviceAuthenticationResult.Cancelled)
    assertTrue(attempt.isCompleted)
  }
}
