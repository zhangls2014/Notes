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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.AccountAvatar
import me.zhangls.profile.api.AccountProfile
import me.zhangls.profile.api.AccountProfileRemote
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileEditError
import me.zhangls.profile.mvi.ProfileField
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.profile.mvi.ProfileViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class ProfileViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()
  private val users = Users()
  private val storage = Storage()
  private val savedState = SavedStateHandle()
  private val viewModelStore = ViewModelStore()
  private lateinit var viewModel: ProfileViewModel
  private val file = KmpFile(Uri.parse("content://test/avatar"))

  @Before fun start() {
    Dispatchers.setMain(dispatcher)
    viewModel = ProfileViewModel(savedState, users, AvatarUpdater(users, storage, dispatcher, null))
    viewModelStore.put("profile", viewModel)
  }

  @After fun stop() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

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
    storage.beforeReturn = {
      started.complete(Unit)
      finish.await()
    }
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5.seconds) { started.await() }
    assertEquals(AvatarSaveStatus.Saving, viewModel.state.value.saveStatus)
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    finish.complete(Unit)
    withTimeout(5.seconds) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Saved } }
    assertEquals(1, storage.saves)
    assertEquals("new", users.userFlow.value?.avatar)
    assertEquals("new", viewModel.state.value.user?.avatar)
  }

  @Test fun failedSaveKeepsOriginalAvatarAndAllowsRetry() = runBlocking {
    storage.path = null
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5.seconds) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Failed } }
    assertEquals("old", viewModel.state.value.user?.avatar)
    storage.path = "new"
    viewModel.sendIntent(ProfileIntent.ChangeAvatar(file))
    withTimeout(5.seconds) { viewModel.state.first { it.saveStatus == AvatarSaveStatus.Saved } }
    assertEquals("new", viewModel.state.value.user?.avatar)
    assertEquals(2, storage.saves)
  }

  @Test fun emailEditRejectsIncorrectPasswordAndIgnoresDuplicateConfirmation() = runBlocking {
    val verification = CompletableDeferred<Boolean>()
    val attempts = mutableListOf<String>()
    val deliveries = mutableListOf<String>()
    val remote = object : AccountProfileRemote {
      override suspend fun getProfile(userId: String) = AccountProfile(userId, "Ada", null)
      override suspend fun getAvatar(userId: String): AccountAvatar = error("Unexpected avatar request")
      override suspend fun updateAvatar(userId: String, jpeg: ByteArray): AccountAvatar = error("Unexpected upload")
      override suspend fun updateUsername(userId: String, username: String): Unit = error("Unexpected rename")
      override suspend fun changeEmail(userId: String, email: String, code: String): Unit = error("Unexpected save")

      override suspend fun verifyEmailChangePassword(userId: String, password: String): Boolean {
        attempts += password
        return password != "wrong" && verification.await()
      }

      override suspend fun sendEmailChangeCode(userId: String, email: String, password: String) {
        assertEquals("correct", password)
        deliveries += email
      }
    }
    users.userFlow.value = UserModel("remote", "Ada", authOrigin = "https://example.test")
    viewModel = ProfileViewModel(savedState, users, AvatarUpdater(users, storage, dispatcher, remote))
    viewModelStore.put("profile", viewModel)
    viewModel.sendIntent(ProfileIntent.OpenEdit(ProfileField.Email))
    viewModel.sendIntent(ProfileIntent.UpdatePassword("wrong"))
    viewModel.sendIntent(ProfileIntent.ConfirmEdit)
    val rejected = withTimeout(5.seconds) { viewModel.state.first { it.edit?.error == ProfileEditError.IncorrectPassword } }
    assertEquals(false, rejected.edit?.passwordVerified)
    viewModel.sendIntent(ProfileIntent.UpdatePassword("correct"))
    viewModel.sendIntent(ProfileIntent.ConfirmEdit)
    viewModel.sendIntent(ProfileIntent.ConfirmEdit)
    assertEquals(true, viewModel.state.value.edit?.busy)
    assertEquals(listOf("wrong", "correct"), attempts)
    verification.complete(true)
    withTimeout(5.seconds) { viewModel.state.first { it.edit?.passwordVerified == true } }
    viewModel.sendIntent(ProfileIntent.UpdatePassword("replacement"))
    viewModel.sendIntent(ProfileIntent.UpdateEmail("new@EXAMPLE.COM"))
    viewModel.sendIntent(ProfileIntent.ConfirmEdit)
    withTimeout(5.seconds) { viewModel.state.first { it.edit?.codeRequested == true } }
    assertEquals(listOf("new@example.com"), deliveries)
    assertTrue(savedState.keys().isEmpty())
    viewModel.sendIntent(ProfileIntent.CloseEdit)
    assertEquals(null, viewModel.state.value.edit)
  }

  private class Storage : AvatarStorage {
    var path: String? = "new"
    var saves = 0
    var beforeReturn: suspend () -> Unit = {}
    override suspend fun save(avatar: KmpFile): String? {
      saves++
      beforeReturn()
      return path
    }
    override fun encodeForUpload(path: String): ByteArray = error("No remote avatar expected")
    override fun storeJpeg(jpeg: ByteArray): String = error("No remote avatar expected")
    override fun exists(path: String) = true
    override suspend fun delete(path: String) = Unit
  }
  private class Users : UserRepository {
    override val userFlow = MutableStateFlow<UserModel?>(UserModel("local", "Ada", "old"))
    override suspend fun getUser() = userFlow.value
    override suspend fun replaceTokens(userId: String, expectedRefreshToken: String, tokens: AuthTokens) = false
    override suspend fun getTokens(): AuthTokens? = null
    override suspend fun login(user: UserModel, tokens: AuthTokens) {
      userFlow.value = user
    }
    override suspend fun updateAvatar(avatar: String, expectedUser: UserModel): Boolean {
      userFlow.value = userFlow.value?.copy(avatar = avatar)
      return true
    }
    override suspend fun updateAccountProfile(
      account: String,
      avatar: String?,
      revision: String?,
      expectedUser: UserModel,
      email: String?,
    ): Boolean = false
    override suspend fun updateBiometricUnlock(enabled: Boolean) = false
    override suspend fun updateEmailSearchHistory(keyword: String) = Unit
    override suspend fun deleteEmailSearchHistory(keyword: String) = Unit
    override suspend fun clear() {
      userFlow.value = null
    }
  }
}
