package me.zhangls.profile.mvi

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.framework.mvi.MviIntent

internal sealed interface ProfileIntent : MviIntent {
  data class ChangeAvatar(val file: KmpFile?) : ProfileIntent
}
