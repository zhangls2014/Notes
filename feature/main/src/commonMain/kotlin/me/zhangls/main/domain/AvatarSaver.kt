package me.zhangls.main.domain

import org.koin.core.annotation.Factory

/**
 * @author zhangls
 */
@Factory
expect class AvatarSaver {
  fun save(avatar: String): String?

  fun delete(path: String)
}