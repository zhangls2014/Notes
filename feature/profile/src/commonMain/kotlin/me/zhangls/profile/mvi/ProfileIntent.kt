package me.zhangls.profile.mvi

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.framework.mvi.MviIntent

internal sealed interface ProfileIntent : MviIntent {
  data class OpenEdit(val field: ProfileField) : ProfileIntent
  data object CloseEdit : ProfileIntent
  data class UpdateUsername(val value: String) : ProfileIntent
  data class UpdatePassword(val value: String) : ProfileIntent {
    override fun toString() = "UpdatePassword"
  }
  data class UpdateEmail(val value: String) : ProfileIntent
  data class UpdateCode(val value: String) : ProfileIntent {
    override fun toString() = "UpdateCode"
  }
  data object ConfirmEdit : ProfileIntent
  data class ChangeAvatar(val file: KmpFile?) : ProfileIntent
}
