package me.zhangls.about

import io.ktor.client.HttpClient
import me.zhangls.download.DownloadRequest
import me.zhangls.download.downloadInBackground
import me.zhangls.download.handleDownloadSessionEvents
import me.zhangls.download.hasActiveBackgroundDownload
import me.zhangls.download.prepareBackgroundDownloads

internal actual fun platformWebAssetDownloader(client: HttpClient): WebAssetDownloader =
  object : WebAssetDownloader {
    override suspend fun prepare(requests: List<DownloadRequest>) = prepareBackgroundDownloads(requests)

    override suspend fun hasActiveDownload(relativePath: String): Boolean = hasActiveBackgroundDownload(relativePath)

    override suspend fun download(request: DownloadRequest, onProgress: (Long) -> Unit) =
      downloadInBackground(request, onProgress)
  }

internal actual suspend fun scheduleWebAssetRefresh(cache: WebAssetCache) {
  cache.refresh()
}

fun handleWebAssetDownloadEvents(identifier: String, completion: () -> Unit) =
  handleDownloadSessionEvents(identifier, completion)
