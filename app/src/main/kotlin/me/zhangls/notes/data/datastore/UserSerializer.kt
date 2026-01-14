package me.zhangls.notes.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.zhangls.notes.data.model.UserModel
import me.zhangls.notes.util.AESUtils
import me.zhangls.notes.util.KeystoreKeys
import java.io.InputStream
import java.io.OutputStream

/**
 * @author zhangls
 */
class UserSerializer : Serializer<UserModel> {
  override val defaultValue: UserModel = UserModel(id = "", nickname = "", accessToken = "")

  override suspend fun readFrom(input: InputStream): UserModel {
    return try {
      val encrypted = input.readBytes().decodeToString()
      val decrypted = AESUtils.decrypt(KeystoreKeys.AES_ALIAS_DATASTORE, encrypted)
      Json.decodeFromString<UserModel>(decrypted)
    } catch (serialization: SerializationException) {
      throw CorruptionException("Unable to read UserModel", serialization)
    }
  }

  override suspend fun writeTo(t: UserModel, output: OutputStream) {
    val encrypted = AESUtils.encrypt(KeystoreKeys.AES_ALIAS_DATASTORE, Json.encodeToString(t))
    output.write(encrypted.encodeToByteArray())
  }
}