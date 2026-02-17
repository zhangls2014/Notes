package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.UserModel
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author zhangls
 */
@Singleton
class UserRepository @Inject constructor(
  @Named("UserDataStore") private val dataStore: DataStore<UserModel?>
) {
  val userFlow: Flow<UserModel?> = dataStore.data

  suspend fun update(user: UserModel?) {
    dataStore.updateData { user }
  }
}