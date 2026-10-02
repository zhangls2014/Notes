package me.zhangls.data.impl.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json


/**
 * 基于 Preferences DataStore 的 JSON 序列化读写封装。
 *
 * 注意：这里只做 JSON 序列化，没有加密，敏感数据不得通过此封装落盘；
 * 登录凭据必须通过内部 SecureTokenStore 保存，不得交给此封装。
 *
 * @author zhangls
 */
internal class AppDataStore<T>(
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
      .map { it[key]?.decodeOrNull() }
      .distinctUntilChanged()
  }

  suspend fun updateData(transform: (t: T?) -> T?) {
    dataStore.edit { prefs ->
      val oldValue = prefs[key]?.decodeOrNull() ?: defaultValue
      val newValue = transform(oldValue)?.encode()
      if (newValue == null) {
        prefs.remove(key)
      } else {
        prefs[key] = newValue
      }
    }
  }

  /**
   * 解析已落盘的 JSON，失败一律返回 null（视为"没有值"）而不是抛异常。
   *
   * 反序列化失败在这里是**可达**的：序列化对象里若含枚举（如 `AppLanguage` / `FontSizeConfig`），
   * 删除或重命名任一枚举常量都会让存量 JSON 不再兼容（kotlinx.serialization 对未知枚举值抛
   * `SerializationException`）。异常会顺着 `dataStore.data` 终止整条 Flow，导致设置页与登录页
   * 再也读不到任何取值（且订阅它的 viewModelScope 一并崩溃）；静默回退到 [defaultValue] 虽会
   * 丢弃用户自定义取值，但至少应用保持可用。
   *
   * 写入路径同样走这里：解析失败时以 [defaultValue] 为基底做更新，而不是让异常中断写入。
   */
  private fun String.decodeOrNull(): T? {
    return runCatching { json.decodeFromString(serializer, this) }.getOrNull()
  }

  private fun T.encode(): String {
    return json.encodeToString(serializer, this)
  }
}