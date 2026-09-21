package me.zhangls.email

import me.zhangls.data.DataModule
import me.zhangls.email.api.EmailNavigation
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single


@Module(includes = [DataModule::class, FrameworkModule::class])
@ComponentScan("me.zhangls.email")
class EmailModule {

  /**
   * 本 feature 对宿主导航的贡献（key 的序列化登记 + DeepLink 匹配）。
   *
   * 登记类型就是契约接口 [NavigationContribution]，组合根用 `getAll` 一处收集。
   *
   * 必须带 [Named]：Koin 的实例索引键是 `(类型, qualifier, 作用域)`，多个 feature 各自登记同
   * 一个接口时，若都没有 qualifier 就会**互相覆盖**，`getAll` 只剩最后一个 —— 症状是部分
   * feature 的路由静默失效（DeepLink 打不开、key 未登记）。各 feature 的 name 因此必须唯一。
   */
  @Single
  @Named("emailNavigationContribution")
  fun emailNavigationContribution(): NavigationContribution = EmailNavigation
}
