package me.zhangls.settings.api

import me.zhangls.framework.mvi.MviEffect

/**
 * 设置页对外的结果。
 *
 * 只有"登出"一条：只有它需要宿主参与导航。设置项本身的变更由 `core:data` 的 settings 流
 * 广播，不经过这里。
 *
 * 早先还预留过一个 `Done`，但从未被构造 —— 属"看着有、其实用不了"的假契约。
 *
 * @author zhangls
 */
sealed interface SettingsResult : MviEffect {
  data object Logout : SettingsResult
}
