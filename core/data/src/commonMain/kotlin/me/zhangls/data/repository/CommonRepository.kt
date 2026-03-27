package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.datastore.SecureDataStore
import me.zhangls.data.model.CommonModel
import org.koin.core.annotation.Singleton


@Singleton
class CommonRepository(prefsDataStore: DataStore<Preferences>) {
  private val dataStore = SecureDataStore(
    name = "common",
    serializer = CommonModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = CommonModel()
  )
  val commonFlow: Flow<CommonModel> = dataStore.read().map { it ?: CommonModel() }


  suspend fun increaseLaunchCount() {
    dataStore.updateData {
      it?.copy(launchCount = it.launchCount + 1)
    }
  }

  suspend fun updateVersionCode(versionCode: Long) {
    dataStore.updateData {
      it?.copy(lastVersionCode = versionCode)
    }
  }
}
