package me.zhangls.main.domain

import org.koin.core.annotation.Factory


@Factory
actual class AvatarSaver {
  actual fun save(avatar: String): String? {
    TODO("Not yet implemented")
  }

  actual fun delete(path: String) {
    TODO("Not yet implemented")
  }
}