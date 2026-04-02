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
      .map { it[key]?.decode() }
      .distinctUntilChanged()
  }

  suspend fun updateData(transform: (t: T?) -> T?) {
    dataStore.edit { prefs ->
      val oldValue = prefs[key]?.decode() ?: defaultValue
      val newValue = transform(oldValue)?.encode()
      if (newValue == null) {
        prefs.remove(key)
      } else {
        prefs[key] = newValue
      }
    }
  }

  private fun String.decode(): T {
    return json.decodeFromString(serializer, this)
  }

  private fun T.encode(): String {
    return json.encodeToString(serializer, this)
  }
}