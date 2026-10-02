package me.zhangls.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.mohamedrejeb.calf.io.KmpFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.profile.mvi.ProfileViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ProfileViewModelTest {
  private val users = Users()
  private val storage = Storage()
  private val savedState = SavedStateHandle()
  private val viewModelStore = ViewModelStore()
  private lateinit var viewModel: ProfileViewModel
  private val file = KmpFile(Uri.parse("content://test/avatar"))

  @Before fun start() {
    Dispatchers.setMain(Dispatchers.Unconfined)
    viewModel = ProfileViewModel(savedState, users, AvatarUpdater(users, storage))
    viewModelStore.put("profile", viewModel)
  }
  @After fun stop() { viewModelStore.clear(); Dispatchers.resetMain() }

  @Test fun cancellingSelectionKeepsAvatarAndDoesNotPersistSensitiveState() {
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(null))
    assertEquals(AvatarSaveStatus.Idle, viewModel.state.value.saveStatus)
    assertEquals("old", users.userFlow.value?.avatar)
    assertEquals(0, storage.saves)
    assertTrue(savedState.keys().isEmpty())
  }

  @Test fun duplicateSelectionWhileSavingIsIgnoredAndSharedUserUpdates() = runBlocking {
    val started = CompletableDeferred<Unit>()
    val finish = CompletableDeferred<Unit>()
    storage.beforeReturn = { started.complete(Unit); finish.await() }
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5_000) { started.await() }
    assertEquals(AvatarSaveStatus.Saving, viewModel.state.value.saveStatus)
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    finish.complete(Unit)
    withTimeout(5_000) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Saved } }
    assertEquals(1, storage.saves)
    assertEquals("new", users.userFlow.value?.avatar)
    assertEquals("new", viewModel.state.value.user?.avatar)
  }

  @Test fun failedSaveKeepsOriginalAvatarAndAllowsRetry() = runBlocking {
    storage.path = null
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5_000) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Failed } }
    assertEquals("old", viewModel.state.value.user?.avatar)
    storage.path = "new"
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5_000) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Saved } }
    assertEquals("new", viewModel.state.value.user?.avatar)
    assertEquals(2, storage.saves)
  }

  private class Storage : AvatarStorage {
    var path: String? = "new"
    var saves = 0
    var beforeReturn: suspend () -> Unit = {}
    override suspend fun save(avatar: KmpFile): String? { saves++; beforeReturn(); return path }
    override suspend fun delete(path: String) = Unit
  }
  private class Users : UserRepository {
    override val userFlow = MutableStateFlow<UserModel?>(UserModel("local", "Ada", "old"))
    override suspend fun getUser() = userFlow.value
    override suspend fun getTokens(): AuthTokens? = null
    override suspend fun login(user: UserModel, tokens: AuthTokens) { userFlow.value = user }
    override suspend fun updateAvatar(avatar: String, expectedUser: UserModel): Boolean {
      userFlow.value = userFlow.value?.copy(avatar = avatar)
      return true
    }
    override suspend fun updateEmailSearchHistory(keyword: String) = Unit
    override suspend fun deleteEmailSearchHistory(keyword: String) = Unit
    override suspend fun clear() { userFlow.value = null }
  }
}
