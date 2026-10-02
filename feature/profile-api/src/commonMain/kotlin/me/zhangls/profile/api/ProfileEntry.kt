package me.zhangls.profile.api

import androidx.compose.runtime.Composable

/** 个人信息功能的入口契约；页面、头像修改和状态由 profile 实现。 */
interface ProfileEntry {
  // 跨模块 Native @Composable 抽象方法不声明默认参数。
  @Composable
  fun Screen(onBackPressed: () -> Unit)
}
