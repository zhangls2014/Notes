package me.zhangls.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import me.zhangls.data.util.AESUtils
import me.zhangls.data.util.KeystoreKeys


class SecureDataStore<T>(
  name: String,
  private val serializer: KSerializer<T>,
  private val dataStore: DataStore<Preferences>,
  private val defaultValue: T?,
) {
  private val key = stringPreferencesKey(name)
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
  }


  fun read(): Flow<T?> {
    return dataStore.data
      .map { it[key]?.decrypted() }
      .distinctUntilChanged()
  }

  suspend fun updateData(transform: (t: T?) -> T?) {
    dataStore.edit { prefs ->
      val oldValue = prefs[key]?.decrypted() ?: defaultValue
      val newValue = transform(oldValue)?.encrypted()
      if (newValue == null) {
        prefs.remove(key)
      } else {
        prefs[key] = newValue
      }
    }
  }

  private fun String.decrypted(): T {
    val decrypted = AESUtils.decrypt(KeystoreKeys.AES_ALIAS_DATASTORE, this)
    return json.decodeFromString(serializer, decrypted)
  }

  private fun T.encrypted(): String {
    val plainText = json.encodeToString(serializer, this)
    return AESUtils.encrypt(KeystoreKeys.AES_ALIAS_DATASTORE, plainText)
  }
}