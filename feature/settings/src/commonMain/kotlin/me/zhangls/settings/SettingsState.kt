package me.zhangls.settings

import kotlinx.serialization.Serializable
import me.zhangls.data.model.SettingsModel
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
@Serializable
data class SettingsState(
  val settings: SettingsModel = SettingsModel(),
  val dialog: DialogState? = null,
) : MviState
