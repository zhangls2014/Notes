package me.zhangls.profile.mvi

import me.zhangls.data.model.UserModel

internal sealed interface ProfileAction {
  data class UpdateUser(val user: UserModel?) : ProfileAction
  data class UpdateEdit(val edit: ProfileEdit?) : ProfileAction
  data class UpdateSaveStatus(val status: AvatarSaveStatus) : ProfileAction
}
