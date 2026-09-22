package me.zhangls.email.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel


/**
 * 邮件详情页：作为独立的导航目的地渲染。
 *
 * 外壳（导航套件）由 `AppShell` 统一提供，本页只渲染内容 —— 页面因此不需要知道
 * "自己是否被外壳包住"：导航套件占用的系统内边距已由外壳消费，页面直接用
 * `Scaffold` 给出的 padding。
 *
 * 窗格位置由宿主的场景策略决定：宽窗口下它是分栏里的详情栏，窄窗口下它就是整页。
 * 两种情况下内容完全一样，只有"要不要给返回键"不同（见 [isDetailPaneShownAlone]）。
 */
@Composable
internal fun EmailDetailScreen(
  emailId: Long,
  onBackPressed: () -> Unit = {},
  viewModel: EmailViewModel = koinViewModel(),
) {
  EmailDetail(
    emailId = emailId,
    viewModel = viewModel,
    // 只有详情独占窗口时才给返回键：宽窗口下列表与详情同屏，返回键没有意义
    // （M3 的列表-详情在双栏态不给详情一侧返回键）
    onBackPressed = onBackPressed.takeIf { isDetailPaneShownAlone() },
  )
}

/**
 * 详情栏当前是否独占窗口（列表栏不可见）。
 *
 * 读场景策略下发的 [LocalListDetailSceneScope] 而不是拿窗口宽度当代理：
 * 代理在"折叠设备上详情被降级为 Levitated"或"directive 因姿态只给一栏"时会判错 ——
 * 而这两件事恰好都是本应用最该做对的多形态场景。
 *
 * 作用域为 `null` 表示当前不是列表-详情场景（单栏策略接管），此时详情必然独占窗口。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun isDetailPaneShownAlone(): Boolean {
  val sceneScope = LocalListDetailSceneScope.current ?: return true
  // 用 targetState 而不是 currentState：预测性返回的拖动过程会改 currentState，
  // 用它会让返回键在拖拽中闪烁。
  val listPaneValue = sceneScope.scaffoldTransitionScope.scaffoldStateTransition.targetState[
    ListDetailPaneScaffoldRole.List
  ]
  return listPaneValue == PaneAdaptedValue.Hidden
}
