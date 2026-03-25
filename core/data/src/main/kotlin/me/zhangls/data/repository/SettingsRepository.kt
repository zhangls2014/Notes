package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class SettingsRepository(
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

  suspend fun updateFontSize(fontSizeConfig: FontSizeConfig) {
    dataStore.updateData {
      it.copy(fontSize = fontSizeConfig)
    }
  }

  suspend fun updateAppLanguage(appLanguage: AppLanguage) {
    dataStore.updateData {
      it.copy(appLanguage = appLanguage)
    }
  }
}
