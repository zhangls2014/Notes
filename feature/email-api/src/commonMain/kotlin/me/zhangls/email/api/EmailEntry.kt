package me.zhangls.email.api

import androidx.compose.runtime.Composable

/**
 * Email 功能对外暴露的入口契约。
 *
 * 兄弟 feature 只依赖本接口（`:feature:email-api`），具体实现由 `:feature:email`
 * 通过 Koin 提供并绑定到该接口，仅 `app`（组合根）依赖实现模块。以此切断 feature 间的直接依赖。
 *
 * @author zhangls
 */
interface EmailEntry {
  /**
   * 以内联方式渲染首页邮件列表（作为主界面的一个 Tab，而非独立导航目的地）。
   *
   * @param isBottomNavigationBar 当前是否为底部导航栏布局，用于处理内容内边距
   */
  @Composable
  fun HomeScreen(isBottomNavigationBar: Boolean)

  /**
   * 以内联方式渲染收藏邮件列表（作为主界面的一个 Tab，而非独立导航目的地）。
   *
   * @param isBottomNavigationBar 当前是否为底部导航栏布局，用于处理内容内边距
   * @param navigateToDetail 点击邮件后的回调，携带邮件 id
   */
  @Composable
  fun FavoritesScreen(
    isBottomNavigationBar: Boolean,
    navigateToDetail: (Long) -> Unit = {},
  )

  /**
   * 渲染邮件详情页（作为独立导航目的地，由导航装配调用）。
   *
   * @param emailId 邮件 id
   * @param onBackPressed 返回上一页的回调
   */
  @Composable
  fun DetailScreen(
    emailId: Long,
    onBackPressed: () -> Unit = {},
  )
}
