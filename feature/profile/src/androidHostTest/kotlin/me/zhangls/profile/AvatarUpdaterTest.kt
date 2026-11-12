package me.zhangls.profile

import android.net.Uri
import com.mohamedrejeb.calf.io.KmpFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.AccountAvatar
import me.zhangls.profile.api.AccountProfile
import me.zhangls.profile.api.AccountProfileRemote
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AvatarUpdaterTest {
  private val events = mutableListOf<String>()
  private val users = Users()
  private val storage = Storage()
  private val file = KmpFile(Uri.parse("content://test/avatar"))

  @Test fun savesBeforePersistingAndDeletesOldOnlyAfterCommit() = runTest {
    AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    assertEquals(listOf("save", "commit:new", "delete:old"), events)
    assertEquals("new", users.getUser()?.avatar)
  }

  @Test fun copyFailureRetainsOldAvatar() = runTest {
    storage.path = null
    assertFailsWith<IllegalStateException> {
      AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    }
    assertEquals("old", users.getUser()?.avatar)
    assertEquals(listOf("save"), events)
  }

  @Test fun persistenceFailureRemovesNewFileAndRetainsOld() = runTest {
    users.failCommit = true
    assertFailsWith<IllegalStateException> {
      AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    }
    assertEquals("old", users.getUser()?.avatar)
    assertEquals(listOf("save", "commit:new", "delete:new"), events)
  }

  @Test fun accountChangeDuringCopyCannotUpdateAnotherUser() = runTest {
    storage.beforeReturn = { users.userFlow.value = UserModel("local", "Another user", "another") }
    assertFailsWith<IllegalStateException> {
      AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    }
    assertEquals("another", users.getUser()?.avatar)
    assertEquals(listOf("save", "commit:new", "delete:new"), events)
  }

  @Test fun cancellationDuringCopyCleansNewFileWithoutChangingProfile() = runTest {
    val started = CompletableDeferred<Unit>()
    val finish = CompletableDeferred<Unit>()
    storage.beforeReturn = {
      started.complete(Unit)
      finish.await()
    }
    val update = async { AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file) }
    started.await()
    update.cancel()
    finish.complete(Unit)
    update.cancelAndJoin()
    assertEquals("old", users.getUser()?.avatar)
    assertEquals(listOf("save", "delete:new"), events)
  }

  @Test fun cleanupFailureAfterCommitDoesNotReportSaveFailure() = runTest {
    storage.failDelete = true
    AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    assertEquals("new", users.getUser()?.avatar)
  }

  @Test fun cleanupCancellationEscapesAfterCommit() = runTest {
    storage.cancelDelete = true
    assertFailsWith<CancellationException> {
      AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), null).update(file)
    }
    assertEquals("new", users.getUser()?.avatar)
  }

  @Test fun refreshDownloadsTheAccountAvatarAndSkipsAnUnchangedCopy() = runTest {
    users.userFlow.value = UserModel("remote", "Cached", "old", authOrigin = "https://test.example")
    val updater = AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), remote())
    updater.refresh()
    updater.refresh()
    assertEquals(listOf("download:remote", "cache", "delete:old"), events)
    assertEquals("Server", users.getUser()?.nickname)
    assertEquals("revision", users.getUser()?.avatarRevision)
  }

  @Test fun rejectedUploadLeavesThePreviouslyCachedAvatar() = runTest {
    users.userFlow.value = UserModel("remote", "Ada", "old", authOrigin = "https://test.example")
    val updater = AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), remote(failUpload = true))
    assertFailsWith<IllegalStateException> { updater.update(file) }
    assertEquals(listOf("save", "encode", "delete:new", "upload:remote"), events)
    assertEquals("old", users.getUser()?.avatar)
  }

  @Test fun successfulUploadCachesTheServerResponse() = runTest {
    users.userFlow.value = UserModel("remote", "Ada", "old", authOrigin = "https://test.example")
    AvatarUpdater(users, storage, StandardTestDispatcher(testScheduler), remote()).update(file)
    assertEquals(listOf("save", "encode", "delete:new", "upload:remote", "cache", "delete:old"), events)
    assertEquals("cached", users.getUser()?.avatar)
    assertEquals("revision", users.getUser()?.avatarRevision)
  }

  private fun remote(failUpload: Boolean = false) = object : AccountProfileRemote {
    override suspend fun updateUsername(userId: String, username: String): Unit = error("Unexpected username change")

    override suspend fun sendEmailChangeCode(userId: String, email: String, password: String): Unit =
      error("Unexpected email change")

    override suspend fun changeEmail(userId: String, email: String, code: String): Unit =
      error("Unexpected email change")

    override suspend fun getProfile(userId: String) = AccountProfile(userId, "Server", "revision")

    override suspend fun getAvatar(userId: String): AccountAvatar {
      events += "download:$userId"
      return AccountAvatar("revision", byteArrayOf(2))
    }

    override suspend fun updateAvatar(userId: String, jpeg: ByteArray): AccountAvatar {
      events += "upload:$userId"
      assertEquals(listOf<Byte>(1), jpeg.toList())
      check(!failUpload)
      return AccountAvatar("revision", byteArrayOf(2))
    }
  }

  private inner class Storage : AvatarStorage {
    var path: String? = "new"
    var beforeReturn: suspend () -> Unit = {}
    var failDelete = false
    var cancelDelete = false
    override suspend fun save(avatar: KmpFile): String? {
      events += "save"
      beforeReturn()
      return path
    }
    override fun encodeForUpload(path: String): ByteArray {
      events += "encode"
      return byteArrayOf(1)
    }
    override fun storeJpeg(jpeg: ByteArray): String {
      events += "cache"
      assertEquals(listOf<Byte>(2), jpeg.toList())
      return "cached"
    }
    override fun exists(path: String) = true
    override suspend fun delete(path: String) {
      events += "delete:$path"
      if (cancelDelete) throw CancellationException("Cleanup cancelled")
      if (failDelete) error("cleanup unavailable")
    }
  }

  private inner class Users : UserRepository {
    override val userFlow = MutableStateFlow<UserModel?>(UserModel("local", "Ada", "old"))
    var failCommit = false
    override suspend fun getUser() = userFlow.value
    override suspend fun replaceTokens(userId: String, expectedRefreshToken: String, tokens: AuthTokens) = false
    override suspend fun getTokens(): AuthTokens? = null
    override suspend fun login(user: UserModel, tokens: AuthTokens) {
      userFlow.value = user
    }
    override suspend fun updateAvatar(avatar: String, expectedUser: UserModel): Boolean {
      events += "commit:$avatar"
      if (failCommit) error("persistence unavailable")
      val current = userFlow.value?.takeIf {
        it.id == expectedUser.id && it.nickname == expectedUser.nickname && it.avatar == expectedUser.avatar
      } ?: return false
      userFlow.value = current.copy(avatar = avatar)
      return true
    }
    override suspend fun updateAccountProfile(
      account: String,
      avatar: String?,
      revision: String?,
      expectedUser: UserModel,
      email: String?,
    ): Boolean {
      val current = userFlow.value?.takeIf {
        it.id == expectedUser.id && it.authOrigin == expectedUser.authOrigin
      } ?: return false
      userFlow.value = current.copy(nickname = account, avatar = avatar, avatarRevision = revision, email = email)
      return true
    }
    override suspend fun updateBiometricUnlock(enabled: Boolean) = false
    override suspend fun updateEmailSearchHistory(keyword: String) = Unit
    override suspend fun deleteEmailSearchHistory(keyword: String) = Unit
    override suspend fun clear() {
      userFlow.value = null
    }
  }
}
