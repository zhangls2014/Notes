package me.zhangls.data.impl.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.lastOrNull
import me.zhangls.data.impl.datastore.AppDataStore
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import org.koin.core.annotation.Singleton

@Singleton(binds = [UserRepository::class])
internal class UserRepositoryImpl(prefsDataStore: DataStore<Preferences>) : UserRepository {
  private val dataStore = AppDataStore(
    name = "user",
    serializer = UserModel.serializer(),
    dataStore = prefsDataStore,
    defaultValue = null
  )

  private companion object {
    const val SEARCH_HISTORY_LIMIT = 10
  }

  override val userFlow: Flow<UserModel?> = dataStore.read()

  override suspend fun getUser(): UserModel? = userFlow.lastOrNull()

  override suspend fun update(user: UserModel) {
    dataStore.updateData { user }
  }

  override suspend fun updateAvatar(avatar: String) {
    dataStore.updateData {
      it?.copy(avatar = avatar)
    }
  }

  override suspend fun updateEmailSearchHistory(keyword: String) {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return

    dataStore.updateData {
      val user = it ?: return@updateData null
      val newHistory = buildList {
        add(normalized)
        addAll(user.emailSearchHistory.filterNot { history -> history == normalized })
      }.take(SEARCH_HISTORY_LIMIT)
      user.copy(emailSearchHistory = newHistory)
    }
  }

  override suspend fun deleteEmailSearchHistory(keyword: String) {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return

    dataStore.updateData {
      val user = it ?: return@updateData null
      user.copy(emailSearchHistory = user.emailSearchHistory.filterNot { history -> history == normalized })
    }
  }

  override suspend fun clear() {
    dataStore.updateData { null }
  }
}
