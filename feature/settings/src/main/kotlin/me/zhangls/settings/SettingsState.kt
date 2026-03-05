package me.zhangls.settings

import kotlinx.serialization.Serializable
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviState
import me.zhangls.settings.domain.Preference

/**
 * @author zhangls
 */
@Serializable
data class SettingsState(
  val preferences: List<Preference<*>> = emptyList(),
  val dialog: DialogState? = null,
) : MviState
