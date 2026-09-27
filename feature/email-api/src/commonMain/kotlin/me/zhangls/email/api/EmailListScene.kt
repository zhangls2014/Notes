package me.zhangls.email.api

/**
 * 邮件列表-详情分栏的场景标识。
 *
 * 首页与收藏页各是一套独立的列表-详情：分属两个场景，分栏状态与选中态互不影响。
 *
 * 它也是"这封邮件是从哪个列表打开的"：详情页的导航 key 带着它，于是详情栏必定回到
 * 它所属的那个列表场景，而不是串进另一个 Tab 的分栏。这是**路由信息**而不是布局信息 ——
 * 与 `LoginDestination.redirectTo` 同理：它决定了返回行为（从收藏打开的详情，退回到收藏）。
 */
enum class EmailListScene {
  /** 首页（全部邮件）。 */
  Home,

  /** 收藏。 */
  Favorites,
}
