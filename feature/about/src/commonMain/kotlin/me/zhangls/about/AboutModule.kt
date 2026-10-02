package me.zhangls.about

import me.zhangls.about.api.AboutNavigation
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [FrameworkModule::class])
@ComponentScan("me.zhangls.about")
class AboutModule {
  @Single
  @Named("aboutNavigationContribution")
  fun aboutNavigationContribution(): NavigationContribution = AboutNavigation
}
