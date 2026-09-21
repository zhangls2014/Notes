package me.zhangls.login

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.login.api.LoginNavigation
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single


@Module(includes = [DataModule::class, FrameworkModule::class])
@ComponentScan("me.zhangls.login")
class LoginModule {

  /**
   * 本 feature 对宿主导航的贡献（登录页 key 的序列化登记）。
   *
   * 登记类型就是契约接口 [NavigationContribution]，组合根用 `getAll` 一处收集；
   * 必须带 [Named]，理由见 `EmailModule`。
   */
  @Single
  @Named("loginNavigationContribution")
  fun loginNavigationContribution(): NavigationContribution = LoginNavigation
}
