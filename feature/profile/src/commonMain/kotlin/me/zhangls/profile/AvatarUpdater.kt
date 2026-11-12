package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.AccountAvatar
import me.zhangls.profile.api.AccountProfileRefresh
import me.zhangls.profile.api.AccountProfileRemote
import org.koin.core.annotation.Singleton

/** 复制、提交、回收顺序统一；离开页面取消时也不会丢失旧头像或遗留新副本。 */
@Singleton
class AvatarUpdater internal constructor(
  private val users: UserRepository,
  private val storage: AvatarStorage,
  private val ioDispatcher: CoroutineDispatcher,
  private val remote: AccountProfileRemote?,
) : AccountProfileRefresh {
  private val mutex = Mutex()

  internal suspend fun update(avatar: KmpFile) = mutex.withLock {
    val user = checkNotNull(users.getUser()) { "No signed-in user" }
    if (user.authOrigin == null) {
      updateLocal(avatar, user)
    } else {
      val service = checkNotNull(remote)
      val jpeg = withContext(NonCancellable + ioDispatcher) {
        val path = checkNotNull(storage.save(avatar)) { "Unable to read avatar" }
        try {
          storage.encodeForUpload(path)
        } finally {
          deleteCopy(path)
        }
      }
      require(jpeg.size in 1..AVATAR_MAX_BYTES)
      val saved = service.updateAvatar(user.id, jpeg)
      cache(user, user.nickname, saved)
    }
  }

  internal suspend fun updateUsername(username: String) = mutex.withLock {
    val user = requireRemoteUser()
    val account = username.trim().lowercase()
    checkNotNull(remote).updateUsername(user.id, account)
    check(users.updateAccountProfile(account, user.avatar, user.avatarRevision, user))
  }

  internal suspend fun sendEmailChangeCode(email: String, password: String) = mutex.withLock {
    val user = requireRemoteUser()
    checkNotNull(remote).sendEmailChangeCode(user.id, email.trim(), password)
  }

  internal suspend fun changeEmail(email: String, code: String) = mutex.withLock {
    val user = requireRemoteUser()
    val recipient = email.trim()
    val address = recipient.substringBeforeLast('@') + "@" + recipient.substringAfterLast('@').lowercase()
    checkNotNull(remote).changeEmail(user.id, address, code)
    check(users.updateAccountProfile(user.nickname, user.avatar, user.avatarRevision, user, email = address))
  }

  private suspend fun requireRemoteUser(): UserModel = checkNotNull(users.getUser()?.takeIf { it.authOrigin != null })

  override suspend fun refresh() {
    try {
      mutex.withLock { refreshProfile() }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // Keep the previous profile available during temporary service or cache failures.
    }
  }

  private suspend fun refreshProfile() {
    val user = users.getUser()?.takeIf { it.authOrigin != null } ?: return
    val service = remote ?: return
    val profile = service.getProfile(user.id)
    check(profile.id == user.id)
    val revision = profile.avatarRevision
    val path = user.avatar
    val email = profile.email ?: user.email
    val identityChanged = profile.account != user.nickname || email != user.email
    if (revision == null) {
      cacheIdentityWithoutRemoteAvatar(user, profile.account, email, identityChanged)
    } else if (revision == user.avatarRevision && path != null && withContext(ioDispatcher) { storage.exists(path) }) {
      if (identityChanged) users.updateAccountProfile(profile.account, path, revision, user, email)
    } else {
      val avatar = service.getAvatar(user.id)
      // Another device may have uploaded since the metadata request. Cache the revision that was downloaded.
      cache(user, profile.account, avatar, email)
    }
  }

  private suspend fun cacheIdentityWithoutRemoteAvatar(
    user: UserModel,
    account: String,
    email: String?,
    identityChanged: Boolean,
  ) {
    // A device-local avatar stays visible until the user replaces it.
    val path = user.avatar
    val retained = path.takeIf { user.avatarRevision == null }
    if (retained == path && !identityChanged) return
    val updated = users.updateAccountProfile(account, retained, null, user, email)
    if (updated && path != null && retained == null) {
      withContext(NonCancellable + ioDispatcher) { deleteCopy(path) }
    }
  }

  private suspend fun cache(user: UserModel, account: String, avatar: AccountAvatar, email: String? = user.email) {
    require(avatar.jpeg.size in 1..AVATAR_MAX_BYTES)
    val caller = currentCoroutineContext()
    withContext(NonCancellable + ioDispatcher) {
      val path = storage.storeJpeg(avatar.jpeg)
      var committed = false
      try {
        caller.ensureActive()
        check(users.updateAccountProfile(account, path, avatar.revision, user, email)) { "Account changed" }
        committed = true
        user.avatar?.let { deleteCopy(it) }
      } finally {
        if (!committed) deleteCopy(path)
      }
    }
  }

  private suspend fun updateLocal(avatar: KmpFile, expectedUser: UserModel) {
    val caller = currentCoroutineContext()
    // 文件复制不可中断。完成复制后检查调用方，再原子提交；清理不能被页面销毁取消。
    withContext(NonCancellable + ioDispatcher) {
      val path = checkNotNull(storage.save(avatar)) { "Unable to save avatar" }
      var committed = false
      try {
        caller.ensureActive()
        check(users.updateAvatar(path, expectedUser)) { "User or avatar changed" }
        committed = true
        expectedUser.avatar?.takeIf { it != path }?.let { old ->
          // 已提交的新头像有效；旧文件回收失败不影响这次保存结果。
          deleteCopy(old)
        }
      } finally {
        if (!committed) deleteCopy(path)
      }
    }
  }

  private suspend fun deleteCopy(path: String) {
    try {
      storage.delete(path)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // 清理失败不覆盖头像提交结果；取消仍由调用方处理。
    }
  }
}
