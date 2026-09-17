package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.datastore.AppDataStore
import me.zhangls.data.model.CommonModel
import org.koin.core.annotation.Singleton

interface CommonRepository {
  val commonFlow: Flow<CommonModel>

  suspend fun increaseLaunchCount()

  suspend fun updateVersionCode(versionCode: Long)
}

@Singleton(binds = [CommonRepository::class])
class CommonRepositoryImpl(prefsDataStore: DataStore<Preferences>) : CommonRepository {
  private val dataStore = AppDataStore(
    name = "common",
    serializer = CommonModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = CommonModel()
  )
  override val commonFlow: Flow<CommonModel> = dataStore.read().map { it ?: CommonModel() }

  override suspend fun increaseLaunchCount() {
    dataStore.updateData {
      it?.copy(launchCount = it.launchCount + 1)
    }
  }

  override suspend fun updateVersionCode(versionCode: Long) {
    dataStore.updateData {
      it?.copy(lastVersionCode = versionCode)
    }
  }
}
