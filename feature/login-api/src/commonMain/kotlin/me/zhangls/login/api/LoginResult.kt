package me.zhangls.login.api

import me.zhangls.framework.mvi.MviEffect

/**
 * 登录页对外的结果。
 *
 * 只有"成功"一条：只有它需要宿主参与导航（决定登录后落到哪一页）。登录失败停留在登录页
 * 就地提示，不构成导航结果。
 *
 * 早先这里还预留了 `Error(message)` 与 `Cancel`，但从未被任何地方构造 —— 属于"看着有、
 * 其实用不了"的假契约：消费方不得不为它们写分支，而分支永远走不到。
 *
 * @author zhangls
 */
sealed interface LoginResult : MviEffect {
  data object Success : LoginResult
}
