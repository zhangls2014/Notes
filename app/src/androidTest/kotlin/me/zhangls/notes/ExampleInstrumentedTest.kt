package me.zhangls.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.zhangls.notes.util.AESUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("me.zhangls.notes", appContext.packageName)
  }

  @Test
  fun testKeystoreAES() {
    val alias = "my_aes_key"

    val plainText = "Hello Keystore AES!"
    val encrypted = AESUtils.encrypt(alias, plainText)
    val decrypted = AESUtils.decrypt(alias, encrypted)

    println("Encrypted: $encrypted")
    println("Decrypted: $decrypted")

    assertEquals(plainText, decrypted)
  }
}