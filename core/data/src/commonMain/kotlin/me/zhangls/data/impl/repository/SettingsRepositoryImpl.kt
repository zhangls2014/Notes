package me.zhangls.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.impl.datastore.AppDataStore
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.model.FontSizeConfig
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import org.koin.core.annotation.Singleton

@Singleton(binds = [SettingsRepository::class])
internal class SettingsRepositoryImpl(prefsDataStore: DataStore<Preferences>) : SettingsRepository {
  private val dataStore = AppDataStore(
    name = "settings",
    serializer = SettingsModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = SettingsModel()
  )

  override val settingsFlow: Flow<SettingsModel> = dataStore.read().map { it ?: SettingsModel() }

  override suspend fun updateDarkTheme(darkThemeConfig: DarkThemeConfig) {
    dataStore.updateData {
      it?.copy(darkTheme = darkThemeConfig)
    }
  }

  override suspend fun updateDynamicColor(useDynamicColor: Boolean) {
    dataStore.updateData {
      it?.copy(dynamicColor = useDynamicColor)
    }
  }

  override suspend fun updateFontSize(fontSizeConfig: FontSizeConfig) {
    dataStore.updateData {
      it?.copy(fontSize = fontSizeConfig)
    }
  }

  override suspend fun updateAppLanguage(appLanguage: AppLanguage) {
    dataStore.updateData {
      it?.copy(appLanguage = appLanguage)
    }
  }
}
