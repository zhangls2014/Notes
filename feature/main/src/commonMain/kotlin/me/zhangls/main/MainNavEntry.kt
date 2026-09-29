package me.zhangls.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailEntry
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.settings.api.SettingsEntry
import me.zhangls.settings.api.SettingsResult
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_msg_select_email
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// listPane 内部的 metadata 按实例比较。复用静态元数据，让弹栈后的列表 Scene
// 与预测返回的目标保持相等，避免松手时动画目标变化并重新播放进入动画。
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val homePaneMetadata = ListDetailSceneStrategy.listPane(
  sceneKey = EmailListScene.Home,
  detailPlaceholder = { DetailPlaceholder() },
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val favoritesPaneMetadata = ListDetailSceneStrategy.listPane(
  sceneKey = EmailListScene.Favorites,
  detailPlaceholder = { DetailPlaceholder() },
)

/**
 * 主界面三个 Tab 的导航条目。
 *
 * "哪个 Tab 放哪个 feature 的内容"是主界面的知识，集中在这里；email 与 settings 只提供内容，
 * 不关心自己被放在哪一个 Tab 里。
 *
 * **首页与收藏是"列表栏"**：它们的条目带 `ListDetailSceneStrategy.listPane` 标注，于是当返回栈里
 * 还有邮件详情（"详情栏"）时，宿主用 Nav3 的场景策略把两者装进同一屏的分栏；窗口不够宽时
 * 场景策略让位给单栏策略，详情就是整页。列表与详情谁在哪一栏由场景策略按窗口形态决定 ——
 * 本文件只声明"我是列表" / "我是详情"，不判断窗口，也不自建 pane scaffold。
 *
 * @param openedDetail 返回栈顶的邮件详情（若有），由宿主从返回栈派生传入。
 *   列表据此渲染"已打开"态；每个 Tab 只在自己那个场景里采用它
 * @param navigateToEmailDetail 打开一封邮件：入参是"从哪个列表打开"与邮件 id
 * @param onLogout 设置页登出
 */
fun EntryProviderScope<NavKey>.mainNavEntries(
  openedDetail: EmailDetailDestination?,
  navigateToEmailDetail: (EmailListScene, Long) -> Unit,
  onLogout: () -> Unit,
) {
  entry<HomeDestination>(
    metadata = homePaneMetadata,
  ) {
    koinInject<EmailEntry>().HomeScreen(
      openedEmailId = openedDetail?.emailIdIn(EmailListScene.Home),
      navigateToDetail = { navigateToEmailDetail(EmailListScene.Home, it) },
    )
  }

  entry<FavoritesDestination>(
    // 与首页使用独立的 sceneKey，保持各自的列表—详情场景。
    metadata = favoritesPaneMetadata,
  ) {
    koinInject<EmailEntry>().FavoritesScreen(
      openedEmailId = openedDetail?.emailIdIn(EmailListScene.Favorites),
      navigateToDetail = { navigateToEmailDetail(EmailListScene.Favorites, it) },
    )
  }

  // 设置页不带窗格标注：场景策略遇到它会返回 null，由单栏策略接管
  entry<SettingsDestination> {
    koinInject<SettingsEntry>().Screen { result ->
      if (result == SettingsResult.Logout) onLogout()
    }
  }
}

/** 详情确实属于给定场景时取其邮件 id，否则为 null（别的场景的详情不该点亮本列表）。 */
private fun EmailDetailDestination.emailIdIn(scene: EmailListScene): Long? =
  emailId.takeIf { this.scene == scene }

/**
 * 宽窗口下详情栏还没有内容时的空态。
 *
 * 宽窗口上列表栏与详情栏是同时存在的，无铰链时列表采用 directive 的默认首选宽度，
 * 详情占安全内容区的剩余宽度；有铰链时物理分区优先。空态避免列表先拉满整幅宽度，
 * 选中第一封邮件时又突然缩窄。
 */
@Composable
private fun DetailPlaceholder() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      text = stringResource(Res.string.main_msg_select_email),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.outline,
    )
  }
}
