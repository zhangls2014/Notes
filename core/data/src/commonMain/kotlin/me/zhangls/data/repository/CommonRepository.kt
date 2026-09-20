package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.CommonModel

interface CommonRepository {
  val commonFlow: Flow<CommonModel>

  suspend fun increaseLaunchCount()

  suspend fun updateVersionCode(versionCode: Long)
}
