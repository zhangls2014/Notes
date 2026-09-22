package me.zhangls.email.home

import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailList
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 首页：全部邮件的列表。
 *
 * 详情**不在**本文件里。它是独立的目的地（`EmailDetailDestination`），由宿主的 Nav3
 * 列表-详情场景策略与列表装配成同一屏。这样"点开邮件"就是一次真实的导航：它进返回栈、
 * 支持预测性返回、Activity 重建后依然在；而原先它藏在本页内部一个私有的
 * `rememberListDetailPaneScaffoldNavigator` 里 —— 于是同一个动作在首页与收藏页表现不同
 * （首页分栏、收藏页整页替换），首页的返回键行为与收藏页也不一致。
 *
 * 平台差异也随之消失：原先双栏骨架要分 Android / iOS 两份 `actual`
 * （Android 用 `NavigableListDetailPaneScaffold`，那是 `androidMain` 专有 API），
 * 现在窗格装配在 `adaptive-navigation3` 的 common 代码里。
 *
 * @param openedEmailId 当前正在详情栏展示的邮件 id，用于列表项的"已打开"态；
 *   由宿主从返回栈派生传入
 * @param navigateToDetail 点击邮件后的回调
 */
@Composable
fun HomeScreen(
  openedEmailId: Long? = null,
  navigateToDetail: (Long) -> Unit = {},
) {
  // key 与收藏页区分。两个 Tab 现在各自是独立目的地、各有 ViewModelStore，所以
  // "一屏一实例"本就成立；显式 key 让它不依赖装饰器的实现细节。
  val viewModel: EmailViewModel = koinViewModel(key = EmailViewModel.KEY_HOME)

  EmailList(
    viewModel = viewModel,
    isFavorite = false,
    openedEmailId = openedEmailId,
    navigateToDetail = navigateToDetail,
  )
}
