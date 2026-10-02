package me.zhangls.profile

import me.zhangls.data.DataModule
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.profile.api.ProfileNavigation
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [DataModule::class, FrameworkModule::class])
@ComponentScan("me.zhangls.profile")
class ProfileModule {
  @Single
  @Named("profileNavigationContribution")
  fun profileNavigationContribution(): NavigationContribution = ProfileNavigation
}
