package me.zhangls.profile.mvi

import kotlinx.serialization.Serializable
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviState

@Serializable
internal enum class AvatarSaveStatus { Idle, Saving, Saved, Failed }

@Serializable
internal data class ProfileState(
  val user: UserModel? = null,
  val saveStatus: AvatarSaveStatus = AvatarSaveStatus.Idle,
) : MviState
