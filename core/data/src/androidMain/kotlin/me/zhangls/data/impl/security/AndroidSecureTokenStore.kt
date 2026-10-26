package me.zhangls.data.impl.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.zhangls.data.util.AESUtils
import org.koin.core.annotation.Singleton
import java.io.File
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException

@Singleton(binds = [SecureTokenStore::class])
internal class AndroidSecureTokenStore(context: Context, private val ioDispatcher: CoroutineDispatcher) :
  SecureTokenStore {
  private val file = AtomicFile(File(context.noBackupFilesDir, "auth-credentials.enc"))
  private val alias = "notes.auth.credentials.v1"

  override suspend fun read(): StoredCredentials? = withContext(ioDispatcher) {
    if (!file.baseFile.exists()) return@withContext null
    try {
      Json.decodeFromString<StoredCredentials>(AESUtils.decrypt(alias, file.readFully().decodeToString()))
    } catch (_: KeyPermanentlyInvalidatedException) {
      resetInvalidCredentials()
    } catch (_: UnrecoverableKeyException) {
      resetInvalidCredentials()
    } catch (_: AEADBadTagException) {
      resetInvalidCredentials()
    } catch (_: SerializationException) {
      resetInvalidCredentials()
    } catch (_: IllegalArgumentException) {
      resetInvalidCredentials()
    }
  }

  override suspend fun write(credentials: StoredCredentials): Unit = withContext(ioDispatcher) {
    val encrypted = AESUtils.encrypt(alias, Json.encodeToString(credentials)).encodeToByteArray()
    val stream = file.startWrite()
    var finished = false
    try {
      stream.write(encrypted)
      file.finishWrite(stream)
      finished = true
    } finally {
      if (!finished) file.failWrite(stream)
    }
  }

  override suspend fun clear(): Unit = withContext(ioDispatcher) { file.delete() }

  private fun resetInvalidCredentials(): StoredCredentials? {
    file.delete()
    KeyStore.getInstance("AndroidKeyStore").apply {
      load(null)
      deleteEntry(alias)
    }
    return null
  }
}
