package me.zhangls.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.zhangls.data.model.UserModel
import me.zhangls.data.util.AESUtils
import me.zhangls.data.util.KeystoreKeys
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * @author zhangls
 */
class UserSerializer : Serializer<UserModel?> {
  override val defaultValue: UserModel? = null

  override suspend fun readFrom(input: InputStream): UserModel? {
    return try {
      val encrypted = input.readBytes().decodeToString()
      val decrypted = AESUtils.decrypt(KeystoreKeys.AES_ALIAS_DATASTORE, encrypted)
      Json.decodeFromString<UserModel>(decrypted)
    } catch (throwable: Throwable) {
      when (throwable) {
        is SerializationException,
        is IllegalArgumentException,
        is GeneralSecurityException -> throw CorruptionException("Unable to read UserModel", throwable)
        else -> throw throwable
      }
    }
  }

  override suspend fun writeTo(t: UserModel?, output: OutputStream) {
    val encrypted = AESUtils.encrypt(KeystoreKeys.AES_ALIAS_DATASTORE, Json.encodeToString(t))
    withContext(Dispatchers.IO) {
      output.write(encrypted.encodeToByteArray())
    }
  }
}