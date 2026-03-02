package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.DarkThemeConfig
import me.zhangls.data.model.SettingsModel
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author zhangls
 */
@Singleton
class SettingsRepository @Inject constructor(
  @Named("SettingsDataStore") private val dataStore: DataStore<SettingsModel>
) {
  val settingsFlow: Flow<SettingsModel> = dataStore.data

  suspend fun update(settings: SettingsModel) {
    dataStore.updateData { settings }
  }

  suspend fun updateDarkTheme(darkThemeConfig: DarkThemeConfig) {
    dataStore.updateData {
      it.copy(darkTheme = darkThemeConfig)
    }
  }

  suspend fun updateDynamicColor(useDynamicColor: Boolean) {
    dataStore.updateData {
      it.copy(dynamicColor = useDynamicColor)
    }
  }
}