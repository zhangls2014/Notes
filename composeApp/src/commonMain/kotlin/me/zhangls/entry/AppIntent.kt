package me.zhangls.entry

import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface AppIntent : MviIntent {
  data class DismissReleaseNotes(val version: String) : AppIntent
}
