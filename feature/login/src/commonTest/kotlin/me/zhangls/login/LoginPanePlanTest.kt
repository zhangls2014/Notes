@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package me.zhangls.login

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 登录页窗格方案的守卫测试。
 *
 * 与 `feature:main` 的形态策略测试同一思路：钉住**决策**而不是渲染 —— 方案由纯函数算出，
 * 不必启动组合、不必真机旋转，也不必找一台折叠设备。这里出错的代价是静默的：
 * 布局看起来正常，只是"宽窗口上品牌区不见了"、"手机被摊成上下半屏"，或者
 * "折叠半开时表单横跨在铰链上"。
 *
 * 尺寸用 dp 给出，交给与库同一套的 `computeWindowSizeClass` 量化（手写档位下限等于把
 * "档位怎么算"这个被测对象写进测试前提）。
 */
class LoginPanePlanTest {

  @Test
  fun compactPhoneShowsFormOnly() {
    val plan = plan(widthDp = 360, heightDp = 800)
    assertEquals(
      PaneAdaptedValue.Hidden,
      plan.value[SupportingPaneScaffoldRole.Main],
      "窄窗口上品牌（Main，leading/左侧）应被收起，表单独占整幅",
    )
    assertEquals(
      PaneAdaptedValue.Expanded,
      plan.value[SupportingPaneScaffoldRole.Supporting],
      "窄窗口上表单（Supporting）应独占",
    )
    assertEquals(false, plan.isBrandPaneShown, "窄窗口上品牌区不显示，logo 应留在表单列")
  }

  @Test
  fun mediumWidthKeepsFormOnly() {
    // 600–839dp 且高度未到 Expanded 时维持单栏。这是**有意**的：
    // 表单 480dp + 栏间 24dp + 品牌区 ≈ 864dp > 839dp，塞不下。
    plan(widthDp = 700, heightDp = 800).also {
      assertEquals(PaneAdaptedValue.Hidden, it.value[SupportingPaneScaffoldRole.Main])
    }
    plan(widthDp = 839, heightDp = 880).also {
      assertEquals(PaneAdaptedValue.Hidden, it.value[SupportingPaneScaffoldRole.Main])
    }
  }

  @Test
  fun expandedWidthShowsBrandPaneSideBySide() {
    // ≥840dp：品牌（Main/左侧）与表单（Supporting/右侧）并排
    plan(widthDp = 840, heightDp = 800).also {
      assertEquals(
        PaneAdaptedValue.Expanded,
        it.value[SupportingPaneScaffoldRole.Main],
        "宽窗口下品牌（leading/左）应被库的 Hide 放行而 Expand",
      )
      assertEquals(
        PaneAdaptedValue.Expanded,
        it.value[SupportingPaneScaffoldRole.Supporting],
        "宽窗口下表单（trailing/右）应 Expand 并排",
      )
      assertTrue(it.isFormPaneSideBySide)
    }
    plan(widthDp = 1066, heightDp = 1200).also {
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Main])
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Supporting])
    }
    plan(widthDp = 1600, heightDp = 900).also {
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Main])
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Supporting])
    }
  }

  @Test
  fun landscapePhoneShowsBrandPaneSideBySide() {
    // 横屏手机 914×411dp：宽度到 Expanded → 品牌左、表单右并排。
    // 这比"把一列塞进 411dp 高"更省竖向空间。
    plan(widthDp = 914, heightDp = 411).also {
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Main])
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Supporting])
    }
  }

  @Test
  fun tallPhoneDoesNotStackBrand() {
    // 411×914dp（Pixel 级竖屏手机）。它高度已到 Expanded，**容量上**满足
    // `maxVerticalPartitions == 2` —— 但它不是折叠设备，没有铰链要避。
    // 早先按"容量"选叠放时这里会被摊成上下半屏：品牌占掉上半屏、两个字段落到下半屏，
    // 中间空出一大段。这是实机（竖屏手机窗口）扫描时发现的回归。
    val (adaptiveInfo, directive) = windowAndDirective(widthDp = 411, heightDp = 914)
    assertEquals(1, directive.maxHorizontalPartitions)
    assertTrue(directive.maxVerticalPartitions >= 2, "本条的前提：竖向容量确实够两栏")
    val plan = loginPanePlan(adaptiveInfo, directive)
    assertEquals(
      PaneAdaptedValue.Hidden,
      plan.value[SupportingPaneScaffoldRole.Main],
      "竖屏手机不是桌面支架，品牌应被收起；不能按竖向容量去叠放",
    )
    assertEquals(
      PaneAdaptedValue.Expanded,
      plan.value[SupportingPaneScaffoldRole.Supporting],
      "窄窗口下表单应独占（被设为当前目的地）",
    )
  }

  @Test
  fun tabletopStacksBrandInsteadOfSplittingColumns() {
    // 折叠半开（桌面支架）：横向铰链把窗口切成两半。此时窗口**同时**具备横排与竖排容量 ——
    // 若不把横向分区压到 1，库会给出两栏并排，两栏各自横跨铰链。
    // 桌面支架下品牌在 Main 位（顶/左），表单（Supporting）Reflow 到同一分区下方，
    // 与原 Stacked「品牌在上、表单在下」的视觉一致。
    val (adaptiveInfo, directive) = windowAndDirective(
      widthDp = 1600, heightDp = 1000, posture = Posture(isTabletop = true),
    )
    assertTrue(directive.maxHorizontalPartitions >= 2, "本条的前提：宽窗口本会拿到两个横向分区")
    val plan = loginPanePlan(adaptiveInfo, directive)
    assertEquals(1, plan.directive.maxHorizontalPartitions, "桌面支架下必须压到单横向分区")
    assertEquals(
      PaneAdaptedValue.Expanded,
      plan.value[SupportingPaneScaffoldRole.Main],
      "桌面支架下品牌留在 Main 位（顶/左），不被 Hide",
    )
    assertEquals(
      PaneAdaptedValue.Reflowed(SupportingPaneScaffoldRole.Main),
      plan.value[SupportingPaneScaffoldRole.Supporting],
      "桌面支架下表单（Supporting）应 Reflow 到 Main 分区下方，而不是与品牌并排跨过铰链",
    )
    assertTrue(plan.isBrandPaneReflowed)
  }

  @Test
  fun tabletopOnNarrowWindowAlsoStacks() {
    // 窄窗口 + 桌面支架同样叠放：这一档与"宽度够不够两栏"无关。
    plan(widthDp = 700, heightDp = 900, posture = Posture(isTabletop = true)).also {
      assertEquals(PaneAdaptedValue.Expanded, it.value[SupportingPaneScaffoldRole.Main])
      assertEquals(
        PaneAdaptedValue.Reflowed(SupportingPaneScaffoldRole.Main),
        it.value[SupportingPaneScaffoldRole.Supporting],
      )
    }
  }

  @Test
  fun verticalHingeReachesTheScaffoldDirective() {
    // 书本式折叠设备半开：竖向分离铰链。库把它算进 directive 的 excludedBounds，
    // 而本页把 directive 原样交给 SupportingPaneScaffold —— 于是窗格会被排进铰链两侧的
    // 分区，而不是横跨铰链。这条断言钉住"铰链几何确实传到了脚手架"。
    // （分区的切法是 ThreePaneScaffold 的职责，不在这里重复断言。）
    val (adaptiveInfo, _) = windowAndDirective(
      widthDp = 674, heightDp = 841,
      posture = Posture(
        hingeList = listOf(
          HingeInfo(
            bounds = Rect(left = 884f, top = 0f, right = 885f, bottom = 2208f),
            isFlat = false,
            isVertical = true,
            isSeparating = true,
            isOccluding = false,
          ),
        ),
      ),
    )
    val plan = loginPanePlan(adaptiveInfo, calculatePaneScaffoldDirective(adaptiveInfo))
    assertTrue(
      plan.directive.excludedBounds.isNotEmpty(),
      "竖向分离铰链应出现在交给脚手架的指令里（HingePolicy 默认 AvoidSeparating）",
    )
  }
}

private fun plan(
  widthDp: Int,
  heightDp: Int,
  posture: Posture = Posture(),
): LoginPanePlan {
  val (adaptiveInfo, directive) = windowAndDirective(widthDp, heightDp, posture)
  return loginPanePlan(adaptiveInfo, directive)
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
