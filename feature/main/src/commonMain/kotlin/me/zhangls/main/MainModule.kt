package me.zhangls.main

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.main.api.MainNavigation
import me.zhangls.network.NetworkModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single


// EmailModule 由组合根（composeApp）引入；main 仅依赖 email 契约（:feature:email-api）
@Module(
  includes = [
    DataModule::class,
    NetworkModule::class,
    FrameworkModule::class
  ]
)
@ComponentScan("me.zhangls.main")
class MainModule {

  /**
   * 本 feature 对宿主导航的贡献（三个 Tab 的 key 登记 + `/home` 匹配）。
   *
   * 登记类型就是契约接口 [NavigationContribution]，组合根用 `getAll` 一处收集；
   * 必须带 [Named]，理由见 `EmailModule`。
   */
  @Single
  @Named("mainNavigationContribution")
  fun mainNavigationContribution(): NavigationContribution = MainNavigation
}
