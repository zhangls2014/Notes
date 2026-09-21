package me.zhangls.login

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

/**
 * 登录页：登录守卫（[me.zhangls.framework.nav.RequireLogin]）拦下未登录访问时的落点。
 *
 * @property redirectTo 被拦下的原始目标；为空表示登录成功后就进默认首屏。
 *
 * 这个字段必须待在 key 里，而不能放在宿主（`AppNavHost`）的 `remember` 中：key 会被
 * `rememberNavBackStack` 连同整个返回栈一起序列化并恢复，而组合内存活不过 Activity 重建。
 * 早先它存在 `pendingDestination` 这个组合状态里，重建后返回栈停在登录页、待跳转目标却已丢失，
 * 于是"被拦下的 DeepLink 目标"会静默退化成"登录后进主页"。
 */
@Serializable
data class LoginDestination(val redirectTo: Destination? = null) : Destination
