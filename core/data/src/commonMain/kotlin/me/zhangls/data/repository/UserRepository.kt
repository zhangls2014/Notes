package me.zhangls.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.lastOrNull
import me.zhangls.data.datastore.SecureDataStore
import me.zhangls.data.model.UserModel
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class UserRepository(prefsDataStore: DataStore<Preferences>) {
  private val dataStore = SecureDataStore(
    name = "user",
    serializer = UserModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = null
  )
  val userFlow: Flow<UserModel?> = dataStore.read()

  suspend fun getUser(): UserModel? = userFlow.lastOrNull()

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
