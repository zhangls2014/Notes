package me.zhangls.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.util.AESUtils
import me.zhangls.data.util.KeystoreKeys
import java.io.InputStream
import java.io.OutputStream

/**
 * @author zhangls
 */
class SettingsSerializer : Serializer<SettingsModel> {
  override val defaultValue: SettingsModel = SettingsModel()

  override suspend fun readFrom(input: InputStream): SettingsModel {
    return try {
      val encrypted = input.readBytes().decodeToString()
      val decrypted = AESUtils.decrypt(KeystoreKeys.AES_ALIAS_DATASTORE, encrypted)
      Json.decodeFromString<SettingsModel>(decrypted)
    } catch (serialization: SerializationException) {
      throw CorruptionException("Unable to read SettingsModel", serialization)
    }
  }

  override suspend fun writeTo(t: SettingsModel, output: OutputStream) {
    val encrypted = AESUtils.encrypt(KeystoreKeys.AES_ALIAS_DATASTORE, Json.encodeToString(t))
    output.write(encrypted.encodeToByteArray())
  }
}