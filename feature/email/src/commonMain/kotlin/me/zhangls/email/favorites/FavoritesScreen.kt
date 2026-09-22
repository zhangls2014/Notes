package me.zhangls.email.favorites

import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailList
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 收藏：收藏邮件的列表。
 *
 * 与首页完全同构 —— 详情同样是独立目的地，由宿主的 Nav3 列表-详情场景策略装配。
 * 早先收藏页走的是"推入一个整页详情目的地"，首页走的是页内私有 navigator，
 * 于是同一个"点开邮件"在大屏上是两种表现；现在两处都由场景策略决定：
 * 窗口够宽就分栏、不够宽就单栏。
 *
 * @param openedEmailId 当前正在详情栏展示的邮件 id，用于列表项的"已打开"态
 * @param navigateToDetail 点击邮件后的回调
 */
@Composable
fun FavoritesScreen(
  openedEmailId: Long? = null,
  navigateToDetail: (Long) -> Unit = {},
) {
  // key 与首页区分，语义同 HomeScreen
  val viewModel: EmailViewModel = koinViewModel(key = EmailViewModel.KEY_FAVORITES)

  EmailList(
    viewModel = viewModel,
    isFavorite = true,
    openedEmailId = openedEmailId,
    navigateToDetail = navigateToDetail,
  )
}
