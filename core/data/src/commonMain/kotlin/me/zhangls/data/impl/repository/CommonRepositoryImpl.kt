package me.zhangls.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.impl.datastore.AppDataStore
import me.zhangls.data.model.CommonModel
import me.zhangls.data.repository.CommonRepository
import org.koin.core.annotation.Singleton

@Singleton(binds = [CommonRepository::class])
internal class CommonRepositoryImpl(prefsDataStore: DataStore<Preferences>) : CommonRepository {
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
