package me.zhangls.about

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.ktor.client.HttpClient
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.minutes

internal actual fun platformWebAssetDownloader(client: HttpClient): WebAssetDownloader = webAssetHttpDownloader(client)

internal actual suspend fun scheduleWebAssetRefresh(cache: WebAssetCache) {
  val context = KoinPlatform.getKoin().get<Context>()
  val request = OneTimeWorkRequestBuilder<WebAssetRefreshWorker>()
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
  WorkManager.getInstance(context).enqueueUniqueWork("notes-web-asset-refresh", ExistingWorkPolicy.KEEP, request)
  // Keep retrying in the background through WorkManager, but start immediately when the app opens.
  cache.refresh()
}

internal class WebAssetRefreshWorker(context: Context, parameters: WorkerParameters) :
  CoroutineWorker(context, parameters) {
  override suspend fun doWork(): Result {
    val cache = KoinPlatform.getKoin().get<WebAssetCache>()
    return if (withTimeoutOrNull(8.minutes) { cache.refresh() } == true) Result.success() else Result.retry()
  }
}
