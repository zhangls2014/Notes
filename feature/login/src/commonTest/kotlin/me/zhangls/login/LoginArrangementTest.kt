@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package me.zhangls.login

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 登录页排布决策的守卫测试。
 *
 * 与 `feature:main` 的形态策略测试同一思路：钉住**决策**而不是渲染 —— 排布由纯函数算出，
 * 不必启动组合、不必真机旋转，就能把各档宽度与折叠姿态覆盖完。这里出错的代价同样是静默的：
 * 布局看起来正常，只是"宽窗口上另一区不见了"或"内容跨过了铰链"。
 *
 * 尺寸用 dp 给出，交给与库同一套的 `computeWindowSizeClass` 量化（手写档位下限等于把
 * "档位怎么算"这个被测对象写进测试前提）。
 */
class LoginArrangementTest {

  @Test
  fun compactPhoneStaysSingleColumn() {
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 360, heightDp = 800))
  }

  @Test
  fun mediumWidthKeepsSingleColumn() {
    // 600–839dp 且高度未到 Expanded 时维持单栏。这是**有意**的：
    // 表单 480dp + 栏间 24dp + 品牌区 ≈ 864dp > 839dp，塞不下。
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 700, heightDp = 800))
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 839, heightDp = 880))
  }

  @Test
  fun expandedWidthSplitsSideBySide() {
    assertEquals(LoginArrangement.SideBySide, arrangement(widthDp = 840, heightDp = 800))
    assertEquals(LoginArrangement.SideBySide, arrangement(widthDp = 1066, heightDp = 1200))
  }

  @Test
  fun extraLargeWidthSplitsSideBySide() {
    assertEquals(LoginArrangement.SideBySide, arrangement(widthDp = 1600, heightDp = 900))
  }

  @Test
  fun narrowAndTallWindowStacksVertically() {
    // 700×1200dp：宽度只够一栏横向分区，但高度已到 Expanded ——
    // directive 的竖排条件正是"maxHorizontalPartitions == 1 且高度为 Expanded"。
    assertEquals(LoginArrangement.Stacked, arrangement(widthDp = 700, heightDp = 1200))
  }

  @Test
  fun stackBoundaryIsHeightExpanded900dp() {
    // 竖排的临界不在宽度而在**高度**：同一宽度下 899dp 仍是单栏、900dp 起变竖排。
    // 钉住它，是因为这条最容易在"改宽度断点"时被顺手改错。
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 700, heightDp = 899))
    assertEquals(LoginArrangement.Stacked, arrangement(widthDp = 700, heightDp = 900))
  }

  @Test
  fun tabletopPostureStacksVerticallyEvenWhenWide() {
    // 折叠半开：横向铰链把窗口切成两半。此时**同时**具备横排（XL → 3 分区）与竖排能力，
    // 必须让竖排胜出 —— 内容跨过铰链，比少用一点横向空间糟糕得多。
    assertEquals(
      LoginArrangement.Stacked,
      arrangement(widthDp = 1600, heightDp = 1000, posture = Posture(isTabletop = true)),
    )
  }

  @Test
  fun landscapePhoneIsNotTwoPane() {
    // 800×400：宽度到 Medium，但高度是 Compact。这条同时钉住两件事 ——
    // "宽度够 ≠ 该分两区"，以及"竖排只在高度真的够时出现"。
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 800, heightDp = 400))
  }
}

private fun arrangement(
  widthDp: Int,
  heightDp: Int,
  posture: Posture = Posture(),
): LoginArrangement = loginArrangementFor(
  calculatePaneScaffoldDirective(
    WindowAdaptiveInfo(
      windowSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp),
      windowPosture = posture,
    )
  )
)
