package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.AboutNavigation
import me.zhangls.about.api.NotesWebsite
import me.zhangls.framework.FrameworkModule
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.network.httpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [FrameworkModule::class])
@ComponentScan("me.zhangls.about")
class AboutModule {
  @Single
  @Named("publicUpdates")
  fun updatesClient(): HttpClient = httpClient {
    followRedirects = false
    install(HttpTimeout) { requestTimeoutMillis = 10_000L }
  }

  @Single
  fun aboutEntry(site: NotesWebsite, @Named("publicUpdates") client: HttpClient): AboutEntry =
    AboutEntryImpl(site, WebAssetCache(site, client)::refresh)

  @Single
  @Named("aboutNavigationContribution")
  fun aboutNavigationContribution(): NavigationContribution = AboutNavigation
}
