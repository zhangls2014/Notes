package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.UserModel
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class UserRepository(
  @Named("UserDataStore") private val dataStore: DataStore<UserModel?>
) {
  val userFlow: Flow<UserModel?> = dataStore.data

  suspend fun update(user: UserModel) {
    dataStore.updateData { user }
  }

  suspend fun updateAvatar(avatar: String) {
    dataStore.updateData {
      it?.copy(avatar = avatar)
    }
  }

  suspend fun clear() {
    dataStore.updateData { null }
  }
}
