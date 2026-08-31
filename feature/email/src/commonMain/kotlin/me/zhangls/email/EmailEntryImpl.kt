package me.zhangls.email

import androidx.compose.runtime.Composable
import me.zhangls.email.api.EmailEntry
import me.zhangls.email.detail.EmailDetailScreen
import me.zhangls.email.favorites.FavoritesScreen as FavoritesScreenImpl
import me.zhangls.email.waterfall.HomeScreen as HomeScreenImpl
import org.koin.core.annotation.Singleton

/**
 * [EmailEntry] 的实现：内联渲染首页邮件列表与收藏列表，并独立渲染邮件详情页。
 *
 * 通过 Koin 绑定到 [EmailEntry] 接口。兄弟 feature（如 main）只依赖 `:feature:email-api`，
 * 运行时由本实现提供，从而切断 feature 间的直接依赖；仅 `app`（组合根）依赖 `:feature:email`。
 *
 * @author zhangls
 */
@Singleton(binds = [EmailEntry::class])
class EmailEntryImpl : EmailEntry {
  @Composable
  override fun HomeScreen(isBottomNavigationBar: Boolean) {
    HomeScreenImpl(isBottomNavigationBar = isBottomNavigationBar)
  }

  @Composable
  override fun FavoritesScreen(
    isBottomNavigationBar: Boolean,
    navigateToDetail: (Long) -> Unit,
  ) {
    FavoritesScreenImpl(
      isBottomNavigationBar = isBottomNavigationBar,
      navigateToDetail = navigateToDetail,
    )
  }

  @Composable
  override fun DetailScreen(
    emailId: Long,
    onBackPressed: () -> Unit,
  ) {
    EmailDetailScreen(
      emailId = emailId,
      onBackPressed = onBackPressed,
    )
  }
}
