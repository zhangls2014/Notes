package me.zhangls.settings

import kotlinx.serialization.Serializable
import me.zhangls.data.model.SettingsModel
import me.zhangls.framework.mvi.MviState
import me.zhangls.settings.domain.SettingsDialog

/**
 * @author zhangls
 */
@Serializable
data class SettingsState(
  val settings: SettingsModel = SettingsModel(),
  val dialog: SettingsDialog? = null,
) : MviState
