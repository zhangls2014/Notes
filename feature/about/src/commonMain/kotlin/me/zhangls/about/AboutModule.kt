package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.AboutNavigation
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.mvi.UpdateController
import me.zhangls.framework.AppInfo
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
  internal fun webAssetCache(site: NotesWebsite, @Named("publicUpdates") client: HttpClient): WebAssetCache =
    WebAssetCache(site, client, platformWebAssetDownloader(client))

  @Single
  internal fun updateController(
    appInfo: AppInfo,
    site: NotesWebsite,
    @Named("publicUpdates") client: HttpClient,
    installer: UpdateInstaller,
  ): UpdateController = UpdateController(
    appInfo,
    site,
    client,
    installer,
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
  )

  @Single
  internal fun aboutEntry(site: NotesWebsite, cache: WebAssetCache, updates: UpdateController): AboutEntry =
    AboutEntryImpl(site, updates::onForegroundChanged) { scheduleWebAssetRefresh(cache) }

  @Single
  @Named("aboutNavigationContribution")
  fun aboutNavigationContribution(): NavigationContribution = AboutNavigation
}
