package me.zhangls.theme.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass

/**
 * 当前窗口的形态信息（尺寸类 + 姿态）。全应用**唯一**的窗口事实来源。
 *
 * 为什么要有这个 CompositionLocal，而不是各消费方直接调用 `currentWindowAdaptiveInfoV2()`：
 *
 *  1. **同一帧内必须一致**。一个窗口只有一种形态，如果 `AppShell` 与某个页面各自算一次，
 *     两次调用之间窗口变了（分屏拖拽、折叠）就会得到互相矛盾的答案。
 *  2. **可替换**。`@Preview` 与测试无法旋转真实窗口；有了这一层，宽屏布局可以像
 *     `ProvideWindowAdaptiveInfo` 那样被直接喂进去，不必依赖 `wm size` 与真机。
 *  3. **契约清晰**。下游读的是"窗口多大"这个事实，而不是"某个组件算出来的结果"。
 *     早先的 `isBottomNavigationBar` 与后来的 `NavigationPlacement` 都是**推导后**的值，
 *     于是各自被当成不同含义复用（内边距 / 搜索栏形态 / 屏幕尺寸）；下发事实不会再出现这种漂移。
 *
 * 没有兜底默认值：读到本值却没人提供，说明这段内容跑在组合根之外 —— 用一个立刻失败的
 * 实现替代静默兜底。"这层包装漏了"晚一点被发现，总好过一个看起来正常、其实按错误形态排出来的布局。
 */
val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> {
  error("LocalWindowAdaptiveInfo 未被提供：窗口形态必须在组合根由 rememberWindowAdaptiveInfo() 提供")
}

/**
 * 读取当前窗口形态。**全应用只应在组合根调用一次**（`composeApp` 的 `App`），
 * 其余地方通过 [LocalWindowAdaptiveInfo] 消费。
 *
 * 用 `currentWindowAdaptiveInfoV2()` 而不是无参的 `currentWindowAdaptiveInfo()`：后者已废弃，
 * 且内部的 `computeFromDpSize` 只支持 `{0, 600, 840}` 三档宽度，档位封顶在 Expanded ——
 * Large / ExtraLarge 根本不存在，任何依赖它们的判断都会静默失效。
 *
 * 调用位置要在主题**之上**：尺寸类是用 `LocalDensity` 把容器像素换算成 dp 得到的
 * （`WindowAdaptiveInfo.kt` 里 `with(LocalDensity.current) { containerSize.toDpSize() }`），
 * 而本应用的主题会覆写 `LocalDensity`（`FontSizeConfig`）。让"窗口多大"依赖"字号设置"
 * 是个隐性错误耦合 —— 当前只改 `fontScale`、`density` 不变所以结果正确，但改 `density`
 * 那天就会静默算错。
 */
@Composable
fun rememberWindowAdaptiveInfo(): WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()

/**
 * 以指定窗口形态渲染内容。仅供 `@Preview` 与测试使用。
 *
 * @param windowSizeClass 伪装的尺寸类，如 `WindowSizeClass(1200, 900)`
 * @param posture 伪装的折叠姿态
 */
@Composable
fun ProvideWindowAdaptiveInfo(
  windowSizeClass: WindowSizeClass,
  posture: Posture = Posture(),
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalWindowAdaptiveInfo provides WindowAdaptiveInfo(windowSizeClass, posture),
    content = content,
  )
}

/**
 * 宽度是否处于 Compact 档（< 600dp，手机竖屏）。
 *
 * 这类语义化读法放在 core 层，而不是让每个消费方自己去比断点常量：消费方关心的
 * 是"有没有横向空间"，断点值属于库的版本细节。
 */
val WindowAdaptiveInfo.isCompactWidth: Boolean
  get() = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** 宽度是否达到 Expanded 档（≥ 840dp，可容纳列表 + 详情两栏）。 */
val WindowAdaptiveInfo.isExpandedWidthOrWider: Boolean
  get() = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

/** 宽度是否达到 ExtraLarge 档（≥ 1600dp，桌面级窗口）。 */
val WindowAdaptiveInfo.isExtraLargeWidthOrWider: Boolean
  get() = windowSizeClass.isWidthAtLeastBreakpoint(
    WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND
  )

/**
 * 当前窗口的窗格排布指令（能并排几栏、能叠放几栏、栏间留白多宽、铰链要避开哪里）。
 *
 * 它是 [LocalWindowAdaptiveInfo] 的**派生**结果，同样只在组合根算一次后下发。之所以也做成
 * CompositionLocal，是因为需要它的地方不止一处（Nav3 的列表-详情场景策略、登录页的两区排布），
 * 而各自算一次意味着**同一份窗口事实有多个派生点**：`calculatePaneScaffoldDirective` 是纯函数，
 * 今天两个调用点必然一致，但参数一旦有人改（换 `HingePolicy`、或改用
 * `calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth`），分叉就发生了 —— 那正是
 * "窗口形态三处各读一次"当初留下的病根，不该以"派生"的形式重来一遍。
 */
val LocalPaneScaffoldDirective = staticCompositionLocalOf<PaneScaffoldDirective> {
  error(
    "LocalPaneScaffoldDirective 未被提供：" +
      "必须由组合根用 rememberPaneScaffoldDirective(rememberWindowAdaptiveInfo()) 提供"
  )
}

/**
 * 由**已经读到的**窗口形态算出窗格排布指令。
 *
 * 刻意接收 [WindowAdaptiveInfo] 参数而不是自己去读窗口：本函数与 [rememberWindowAdaptiveInfo]
 * 都调用在组合根，若它内部再读一次窗口，同一帧里就有两次窗口读取 —— 分屏拖拽或折叠时两次
 * 结果可能不同，而"同一个窗口只有一种形态"是这个模块存在的前提。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberPaneScaffoldDirective(adaptiveInfo: WindowAdaptiveInfo): PaneScaffoldDirective =
  remember(adaptiveInfo) { calculatePaneScaffoldDirective(adaptiveInfo) }

/**
 * 当前窗口能否**并排**放两个窗格（Expanded 及以上，≥ 840dp 宽度）。
 *
 * 语义化读法同样收在这里：消费方问的是"有没有横向空间放两区"，而不是
 * "分区数是不是大于等于 2"。分区数的算法（以及它在 Medium 档的取值）属于库的版本细节。
 */
val PaneScaffoldDirective.canShowSideBySidePanes: Boolean
  get() = maxHorizontalPartitions >= 2

/**
 * 当前窗口能否**上下叠放**两个窗格：桌面支架姿态（折叠半开，横向铰链把窗口切成两半），
 * 或"窄而高"的窗口（只有一栏横向分区、但高度已到 Expanded）。
 *
 * 这一档优先级高于 [canShowSideBySidePanes]：桌面支架下横向铰链把窗口拦腰切断，
 * 内容跨过铰链比"少用一点空间"糟糕得多。
 */
val PaneScaffoldDirective.canShowStackedPanes: Boolean
  get() = maxVerticalPartitions >= 2
