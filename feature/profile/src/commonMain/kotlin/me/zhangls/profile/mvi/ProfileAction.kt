package me.zhangls.profile.mvi

import me.zhangls.data.model.UserModel

internal sealed interface ProfileAction {
  data class UpdateUser(val user: UserModel?) : ProfileAction
  data class UpdateSaveStatus(val status: AvatarSaveStatus) : ProfileAction
}
