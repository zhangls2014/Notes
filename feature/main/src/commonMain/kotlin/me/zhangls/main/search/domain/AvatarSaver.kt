package me.zhangls.main.search.domain

import com.mohamedrejeb.calf.io.KmpFile
import org.koin.core.annotation.Factory

/**
 * @author zhangls
 */
@Factory
expect class AvatarSaver {
  suspend fun save(avatar: KmpFile): String?

  suspend fun delete(path: String)
}
