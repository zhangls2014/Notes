package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.datastore.SecureDataStore
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class SettingsRepository(prefsDataStore: DataStore<Preferences>) {
  private val dataStore = SecureDataStore(
    name = "settings",
    serializer = SettingsModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = SettingsModel()
  )
  val settingsFlow: Flow<SettingsModel> = dataStore.read().map { it ?: SettingsModel() }

  suspend fun updateDarkTheme(darkThemeConfig: DarkThemeConfig) {
    dataStore.updateData {
      it?.copy(darkTheme = darkThemeConfig)
    }
  }

  suspend fun updateDynamicColor(useDynamicColor: Boolean) {
    dataStore.updateData {
      it?.copy(dynamicColor = useDynamicColor)
    }
  }

  suspend fun updateFontSize(fontSizeConfig: FontSizeConfig) {
    dataStore.updateData {
      it?.copy(fontSize = fontSizeConfig)
    }
  }

  suspend fun updateAppLanguage(appLanguage: AppLanguage) {
    dataStore.updateData {
      it?.copy(appLanguage = appLanguage)
    }
  }
}
