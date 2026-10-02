package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.zhangls.data.repository.UserRepository
import org.koin.core.annotation.Factory

/** 复制、提交、回收顺序统一；离开页面取消时也不会丢失旧头像或遗留新副本。 */
@Factory
internal class AvatarUpdater(
  private val users: UserRepository,
  private val storage: AvatarStorage,
) {
  suspend fun update(avatar: KmpFile) {
    val expectedUser = checkNotNull(users.getUser()) { "No signed-in user" }
    val caller = currentCoroutineContext()
    // 文件复制不可中断。完成复制后检查调用方，再原子提交；清理不能被页面销毁取消。
    withContext(NonCancellable + Dispatchers.IO) {
      val path = checkNotNull(storage.save(avatar)) { "Unable to save avatar" }
      var committed = false
      try {
        caller.ensureActive()
        check(users.updateAvatar(path, expectedUser)) { "User or avatar changed" }
        committed = true
        expectedUser.avatar?.takeIf { it != path }?.let { old ->
          // 已提交的新头像有效；旧文件回收失败不影响这次保存结果。
          runCatching { storage.delete(old) }
        }
      } finally {
        if (!committed) runCatching { storage.delete(path) }
      }
    }
  }
}
