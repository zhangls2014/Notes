package me.zhangls.theme.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 导航套件（`NavigationSuiteScaffold`）当前占用的方位。
 *
 * 这是宿主布局对内容区唯一的承诺：**内容的哪一侧已经被导航占用了**。内容区据此决定
 * 自己 Scaffold 的内边距该保留哪几边（见 [toContentPadding]）。
 *
 * 为什么不是"当前是否为底部导航栏"的布尔值：那个说法把布局细节写进了契约 —— 它是
 * "所有可能的布局里，只有底部导航栏一种"这个假设的间接表达，一旦外层改成分栏或多列就失效。
 * 而这里只表达"哪一侧被占"，对现有两种布局都成立。
 */
enum class NavigationPlacement {
  /** 底部导航栏（紧凑窗口）：内容**底部**由导航栏承担，左右仍归内容。 */
  Bottom,

  /** 侧边导航栏 / Rail（宽窗口）：内容**左侧**由 Rail 承担，底部与右侧仍归内容。 */
  Side,
}

/**
 * 由宿主（`feature:main`）提供的 [NavigationPlacement]。
 *
 * 默认取 [NavigationPlacement.Bottom]：脱离主界面单独渲染的页面（如作为独立导航目的地的
 * 详情页）读不到宿主提供的值，此时按紧凑布局兜底最接近它们的实际形态。
 */
val LocalNavigationPlacement = staticCompositionLocalOf { NavigationPlacement.Bottom }

/**
 * 把 Scaffold 给出的系统内边距转换为**内容内边距**：导航套件已占用的那一侧归零，
 * 避免内容与导航栏重复留白。
 *
 * 同一份"哪几边保留"的判断原先在三个消费方各写了一遍，并且已经漂移（同一份逻辑的两个副本
 * 在小屏下对 bottom 的处理不同）。收敛到这里后，新增页面只需调用本函数。
 *
 * @param placement 由 [LocalNavigationPlacement] 读到的导航方位
 */
fun PaddingValues.toContentPadding(placement: NavigationPlacement): PaddingValues {
  val ltr = LayoutDirection.Ltr
  return when (placement) {
    NavigationPlacement.Bottom -> PaddingValues(
      top = calculateTopPadding(),
      // 底部由导航栏承担，内容不再重复留白
      bottom = 0.dp,
      start = calculateStartPadding(ltr),
      end = calculateEndPadding(ltr),
    )

    NavigationPlacement.Side -> PaddingValues(
      top = calculateTopPadding(),
      bottom = calculateBottomPadding(),
      // 左侧由 Rail 承担
      start = 0.dp,
      end = calculateEndPadding(ltr),
    )
  }
}
