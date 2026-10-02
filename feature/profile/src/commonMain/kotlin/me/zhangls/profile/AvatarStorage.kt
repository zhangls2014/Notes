package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile

internal interface AvatarStorage {
  suspend fun save(avatar: KmpFile): String?
  suspend fun delete(path: String)
}
