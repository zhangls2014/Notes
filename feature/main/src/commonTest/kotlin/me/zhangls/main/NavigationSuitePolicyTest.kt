package me.zhangls.main

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import me.zhangls.theme.layout.isCompactWidth
import me.zhangls.theme.layout.isExpandedWidthOrWider
import me.zhangls.theme.layout.isExtraLargeWidthOrWider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 窗口形态策略的守卫测试。
 *
 * 被钉住的是**决策**而不是渲染：形态由纯函数算出，所以不必启动组合、不必真机旋转，
 * 就能把四档宽度与折叠姿态全部覆盖。之所以值得写，是因为这里出过的两个错都不会崩、
 * 不会报错，只是静默地给出错误布局：
 *
 * 1. `WideNavigationRailExpanded` 曾经**永远不可达** —— 旧口径 `currentWindowAdaptiveInfo()`
 *    走的是只含 `{0, 600, 840}` 的 V1 档位集，`minWidthDp` 到不了 1200，
 *    而判断写的是 `isWidthAtLeastBreakpoint(1200)`（即 `minWidthDp >= 1200`），恒假。
 * 2. 反过来，"拿 `minWidthDp` 与 `minHeightDp` 比大小当作横竖屏判断"也是静默错的 ——
 *    这两个值是**分档下限**，不是窗口宽高。
 *
 * 用例里的尺寸用 dp 给出，并交给与库同一套的 `computeWindowSizeClass` 量化，
 * 而不是手写档位下限：手写下限会把"档位怎么算"这个被测对象本身写进测试前提。
 */
class NavigationSuitePolicyTest {

  @Test
  fun compactWidthUsesShortBottomBar() {
    assertEquals(
      NavigationSuiteType.ShortNavigationBarCompact,
      window(widthDp = 360, heightDp = 800).navigationSuiteType(),
    )
  }

  @Test
  fun mediumWidthWithCompactHeightUsesCollapsedRail() {
    // 宽度充足而高度紧凑时，侧栏为列表与正文释放纵向空间。
    assertEquals(
      NavigationSuiteType.WideNavigationRailCollapsed,
      window(widthDp = 800, heightDp = 400).navigationSuiteType(),
    )
  }

  @Test
  fun compactHeightRailRespectsWidthBoundary() {
    assertEquals(NavigationSuiteType.ShortNavigationBarCompact, window(599, 400).navigationSuiteType())
    assertEquals(NavigationSuiteType.WideNavigationRailCollapsed, window(600, 400).navigationSuiteType())
  }

  @Test
  fun compactHeightTakesPriorityOverExtraLargeExpansion() {
    assertEquals(NavigationSuiteType.WideNavigationRailCollapsed, window(1600, 479).navigationSuiteType())
    assertEquals(NavigationSuiteType.WideNavigationRailExpanded, window(1600, 480).navigationSuiteType())
  }

  @Test
  fun mediumWidthTabletUsesCollapsedRail() {
    // 700×1000 竖屏平板：宽度档位到 Medium 即用 Rail（M3 的导航套件口径）
    assertEquals(
      NavigationSuiteType.WideNavigationRailCollapsed,
      window(widthDp = 700, heightDp = 1000).navigationSuiteType(),
    )
  }

  @Test
  fun expandedWidthUsesCollapsedRail() {
    assertEquals(
      NavigationSuiteType.WideNavigationRailCollapsed,
      window(widthDp = 1000, heightDp = 800).navigationSuiteType(),
    )
  }

  @Test
  fun largeWidthStillUsesCollapsedRail() {
    // 1200dp 是 Large 档而不是 ExtraLarge：本应用只在 ≥1600dp 才展开 Rail
    assertEquals(
      NavigationSuiteType.WideNavigationRailCollapsed,
      window(widthDp = 1200, heightDp = 900).navigationSuiteType(),
    )
  }

  @Test
  fun extraLargeWidthUsesExpandedRail() {
    // 这一档就是原先永不可达的死分支：1600dp 窗口必须拿到展开 Rail
    assertEquals(
      NavigationSuiteType.WideNavigationRailExpanded,
      window(widthDp = 1600, heightDp = 900).navigationSuiteType(),
    )
  }

  @Test
  fun v1BreakpointsCannotExpressExtraLarge() {
    // 把根因钉成断言，而不是只写在注释里。
    // 用 V1 档位集量化时，1600dp 窗口的 `minWidthDp` 只有 840，而
    // `isWidthAtLeastBreakpoint(1200)` 的实现是 `minWidthDp >= 1200` —— 恒假。
    // 这正是 `WideNavigationRailExpanded` 曾经永远不可达的原因，
    // 也是"必须用 V2 口径"的理由。若这条断言某天失败，说明库改了量化方式，
    // 上面那条 XL 用例的前提需要重新审视。
    val v1 = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp = 1600, heightDp = 900)
    assertEquals(840, v1.minWidthDp)
    assertFalse(v1.isWidthAtLeastBreakpoint(1200))

    // 对照：V2 量化下同一窗口可以表达 Large / ExtraLarge
    val v2 = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp = 1600, heightDp = 900)
    assertEquals(1600, v2.minWidthDp)
  }

  @Test
  fun foldingStateDoesNotMoveNavigationOrContent() {
    for ((width, height) in listOf(400 to 800, 1000 to 800, 1600 to 1000, 900 to 400)) {
      val flat = window(width, height)
      val half = window(width, height, Posture(
        isTabletop = true,
        hingeList = listOf(HingeInfo(Rect(0f, 390f, width.toFloat(), 410f), false, false, true, false)),
      ))
      assertEquals(flat.navigationSuiteType(), half.navigationSuiteType())
    }
  }

}

/**
 * 语义化宽度读法的边界。搜索栏形态（紧凑 → 全屏 / 否则 docked）与
 * 外壳的展开 Rail 都直接建在这三个属性上，边界值必须钉住。
 */
class WindowWidthSemanticsTest {

  @Test
  fun compactWidthBoundaryIs600dp() {
    assertTrue(window(widthDp = 599, heightDp = 800).isCompactWidth)
    assertFalse(window(widthDp = 600, heightDp = 800).isCompactWidth)
  }

  @Test
  fun expandedWidthBoundaryIs840dp() {
    assertFalse(window(widthDp = 839, heightDp = 800).isExpandedWidthOrWider)
    assertTrue(window(widthDp = 840, heightDp = 800).isExpandedWidthOrWider)
  }

  @Test
  fun extraLargeWidthBoundaryIs1600dp() {
    assertFalse(window(widthDp = 1599, heightDp = 800).isExtraLargeWidthOrWider)
    assertTrue(window(widthDp = 1600, heightDp = 800).isExtraLargeWidthOrWider)
  }
}

/**
 * 从真实 dp 尺寸构造窗口形态，量化方式与库一致
 * （`WindowSizeClass.BREAKPOINTS_V2`）。
 */
private fun window(
  widthDp: Int,
  heightDp: Int,
  posture: Posture = Posture(),
): WindowAdaptiveInfo = WindowAdaptiveInfo(
  windowSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp),
  windowPosture = posture,
)
