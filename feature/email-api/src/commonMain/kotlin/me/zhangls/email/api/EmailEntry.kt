package me.zhangls.email.api

import androidx.compose.runtime.Composable

/**
 * Email 功能对外暴露的入口契约。
 *
 * 兄弟 feature 只依赖本接口（`:feature:email-api`），具体实现由 `:feature:email`
 * 通过 Koin 提供并绑定到该接口，仅 `app`（组合根）依赖实现模块。以此切断 feature 间的直接依赖。
 *
 * 内容内边距不由本契约传递：导航套件占用的那部分系统内边距由应用外壳消费掉
 * （`AppShell` 的 `consumeWindowInsets`），页面直接用 `Scaffold` 给出的 padding。
 * 契约里因此没有布局细节 —— 既没有"导航在哪一侧"，也没有窗口尺寸。
 *
 * @author zhangls
 */
interface EmailEntry {
  // 跨模块 @Composable 抽象方法不声明默认参数：当前 Native 编译链会为实现生成
  // IrLinkageError 占位方法。默认值仅留在模块内部的顶层 Screen 函数中。
  /**
   * 以内联方式渲染首页邮件列表（作为主界面的一个 Tab，而非独立导航目的地）。
   *
   * 只渲染**列表**：详情栏由宿主用 Nav3 的列表-详情场景策略装配 —— 详情的渲染与返回
   * 都随导航返回栈走，所以"点开邮件"必须表达成一次真实的导航，而不是本模块内部
   * 自己维护的一个私有 navigator。
   *
   * @param openedEmailId 当前正在详情栏展示的邮件 id，用于列表项的"已打开"态；
   *   由宿主从返回栈派生并传入（返回栈在宿主手里）
   * @param navigateToDetail 点击邮件后的回调，携带邮件 id
   */
  @Composable
  fun HomeScreen(
    openedEmailId: Long?,
    navigateToDetail: (Long) -> Unit,
  )

  /**
   * 以内联方式渲染收藏邮件列表（作为主界面的一个 Tab，而非独立导航目的地）。
   *
   * @param openedEmailId 当前正在详情栏展示的邮件 id，语义同 [HomeScreen]
   * @param navigateToDetail 点击邮件后的回调，携带邮件 id
   */
  @Composable
  fun FavoritesScreen(
    openedEmailId: Long?,
    navigateToDetail: (Long) -> Unit,
  )

  /**
   * 渲染邮件详情页（作为独立导航目的地，由导航装配调用）。
   *
   * @param emailId 邮件 id
   * @param onBackPressed 返回上一页的回调。**是否显示返回键由实现自己判断**：
   *   宽窗口下列表与详情同屏，详情一侧不该有返回键；只有详情独占窗口时才有意义
   */
  @Composable
  fun DetailScreen(
    emailId: Long,
    onBackPressed: () -> Unit,
  )
}
