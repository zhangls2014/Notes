package me.zhangls.notes.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.notes.data.model.SettingsModel
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

  suspend fun updateDarkMode(isDarkMode: Boolean?) {
    dataStore.updateData {
      it.copy(isDarkMode = isDarkMode)
    }
  }
}