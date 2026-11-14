package me.zhangls.about

import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Singleton

@Singleton
internal actual class UpdateInstaller {
  actual fun observeDownload(apkUrl: String) = flowOf(DownloadStatus.Idle)
  actual suspend fun hasOlderDownload(apkUrl: String) = false
  actual suspend fun enqueue(apkUrl: String) = false
  actual suspend fun install(apkUrl: String, manifest: UpdateManifest) = InstallResult.Failed
}
