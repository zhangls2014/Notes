@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package me.zhangls.login

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 登录页排布决策的守卫测试。
 *
 * 与 `feature:main` 的形态策略测试同一思路：钉住**决策**而不是渲染 —— 排布由纯函数算出，
 * 不必启动组合、不必真机旋转，也不必找一台折叠设备。这里出错的代价同样是静默的：
 * 布局看起来正常，只是"宽窗口上另一区不见了"或"手机被摊成上下半屏"。
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
  fun landscapePhoneSplitsSideBySide() {
    // 横屏手机 914×411dp：宽度到 Expanded → 品牌在左、表单在右。
    // 这比"把一列塞进 411dp 高"更省竖向空间。
    assertEquals(LoginArrangement.SideBySide, arrangement(widthDp = 914, heightDp = 411))
  }

  @Test
  fun tallPhoneStaysSingleColumn() {
    // 411×914dp（Pixel 级竖屏手机）。它高度已到 Expanded，**容量上**也满足
    // `maxVerticalPartitions == 2` —— 但它不是折叠设备，没有铰链要避。
    // 早先按"容量"判断时这里会被摊成上下半屏：品牌占掉上半屏、两个字段落到下半屏，
    // 中间空出一大段。这是实机（竖屏手机窗口）扫描时发现的回归。
    val (adaptiveInfo, directive) = windowAndDirective(widthDp = 411, heightDp = 914)
    assertEquals(1, directive.maxHorizontalPartitions)
    assertTrue(directive.maxVerticalPartitions >= 2, "本条的前提：竖向容量确实够两栏")
    assertFalse(adaptiveInfo.windowPosture.isTabletop)
    assertEquals(LoginArrangement.Single, loginArrangementFor(adaptiveInfo, directive))
  }

  @Test
  fun verticalCapacityIsNotAReasonToStack() {
    // 把"容量 ≠ 理由"直接钉成断言：700×1200dp 的普通窗口同样拿到两个竖向分区，
    // 仍应单栏。判据是姿态（有没有横向铰链），不是 directive 给的容量数字。
    val (adaptiveInfo, directive) = windowAndDirective(widthDp = 700, heightDp = 1200)
    assertTrue(directive.maxVerticalPartitions >= 2)
    assertEquals(LoginArrangement.Single, loginArrangementFor(adaptiveInfo, directive))
  }

  @Test
  fun tabletopPostureStacksVerticallyEvenWhenWide() {
    // 折叠半开（桌面支架）：横向铰链把窗口切成两半。此时**同时**具备横排与竖排容量，
    // 必须让竖排胜出 —— 内容跨过铰链，比少用一点横向空间糟糕得多。
    assertEquals(
      LoginArrangement.Stacked,
      arrangement(widthDp = 1600, heightDp = 1000, posture = Posture(isTabletop = true)),
    )
  }

  @Test
  fun tabletopPostureStacksOnNarrowWindowToo() {
    // 窄窗口 + 桌面支架同样竖排：这一档与"宽度够不够两栏"无关。
    assertEquals(
      LoginArrangement.Stacked,
      arrangement(widthDp = 700, heightDp = 900, posture = Posture(isTabletop = true)),
    )
  }

  @Test
  fun landscapePhoneIsNotTwoPane() {
    // 800×400：宽度到 Medium，但高度是 Compact。这条同时钉住两件事 ——
    // "宽度够 ≠ 该分两区"，以及"竖排只在真的被铰链切开时出现"。
    assertEquals(LoginArrangement.Single, arrangement(widthDp = 800, heightDp = 400))
  }
}

private fun arrangement(
  widthDp: Int,
  heightDp: Int,
  posture: Posture = Posture(),
): LoginArrangement {
  val (adaptiveInfo, directive) = windowAndDirective(widthDp, heightDp, posture)
  return loginArrangementFor(adaptiveInfo, directive)
}

private fun windowAndDirective(
  widthDp: Int,
  heightDp: Int,
  posture: Posture = Posture(),
): Pair<WindowAdaptiveInfo, PaneScaffoldDirective> {
  val adaptiveInfo = WindowAdaptiveInfo(
    windowSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp),
    windowPosture = posture,
  )
  return adaptiveInfo to calculatePaneScaffoldDirective(adaptiveInfo)
}
