package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.CommonModel
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton


@Singleton
class CommonRepository(
  @Named("CommonDataStore") private val dataStore: DataStore<CommonModel>
) {
  val commonFlow: Flow<CommonModel> = dataStore.data


  suspend fun increaseLaunchCount() {
    dataStore.updateData {
      it.copy(launchCount = it.launchCount + 1)
    }
  }

  suspend fun updateVersionCode(versionCode: Long) {
    dataStore.updateData {
      it.copy(lastVersionCode = versionCode)
    }
  }
}
