package me.zhangls.about

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Singleton
internal actual class UpdateInstaller(context: Context) {
  private val app = context.applicationContext
  private val downloads by lazy { app.getSystemService(DownloadManager::class.java) }
  private val downloadLock = Mutex()

  actual fun observeDownload(apkUrl: String) = flow {
    do {
      val status = findDownload(apkUrl)?.status ?: DownloadStatus.Idle
      emit(status)
      if (status == DownloadStatus.Downloading) delay(1.seconds)
    } while (status == DownloadStatus.Downloading)
  }.distinctUntilChanged().flowOn(Dispatchers.IO)

  actual suspend fun hasOlderDownload(apkUrl: String): Boolean =
    withContext(Dispatchers.IO) { olderDownloads(apkUrl).isNotEmpty() }

  actual suspend fun enqueue(apkUrl: String): Boolean = withContext(Dispatchers.IO) {
    downloadLock.withLock {
      val accepted = when (findDownload(apkUrl)?.status) {
        DownloadStatus.Downloading, DownloadStatus.Ready -> true
        else -> enqueueDownload(apkUrl)
      }
      if (accepted) {
        olderDownloads(apkUrl).forEach { downloads.remove(it) }
      }
      accepted
    }
  }

  private fun enqueueDownload(apkUrl: String): Boolean = try {
    val uri = Uri.parse(apkUrl)
    val filename = requireNotNull(uri.lastPathSegment)
    val request = DownloadManager.Request(uri)
      .setTitle(filename)
      .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, "${UUID.randomUUID()}-$filename")
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
    downloads.enqueue(request) >= 0L
  } catch (_: IllegalArgumentException) {
    false
  } catch (_: IllegalStateException) {
    false
  } catch (_: SecurityException) {
    false
  }

  actual suspend fun install(apkUrl: String, manifest: UpdateManifest): InstallResult = try {
    if (!app.packageManager.canRequestPackageInstalls()) {
      app.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", app.packageName, null))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      )
      InstallResult.PermissionRequired
    } else {
      val apk = withContext(Dispatchers.IO) { prepareApk(apkUrl, manifest) }
      val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk)
      app.startActivity(
        Intent(Intent.ACTION_VIEW)
          .setDataAndType(uri, "application/vnd.android.package-archive")
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
      )
      InstallResult.HandedOff
    }
  } catch (_: IOException) {
    InstallResult.Failed
  } catch (_: ActivityNotFoundException) {
    InstallResult.Failed
  } catch (_: SecurityException) {
    InstallResult.Failed
  } catch (_: IllegalArgumentException) {
    InstallResult.Failed
  }

  private fun findDownload(apkUrl: String): Download? =
    downloadRecords().filter { it.url == apkUrl }.maxByOrNull { it.id }

  private fun olderDownloads(apkUrl: String): List<Long> {
    val path = Uri.parse(apkUrl)
    val build = path.pathSegments.getOrNull(1)?.toIntOrNull() ?: return emptyList()
    val prefix = "${path.scheme}://${path.encodedAuthority}/$MAINLAND_UPDATE_CHANNEL/"
    val updatePath = Regex(Regex.escape(prefix) + """([1-9]\d*)/Notes-\d+\.\d+\.\d+-mainland\.apk""")
    return downloadRecords().mapNotNull { download ->
      val olderBuild = updatePath.matchEntire(download.url)?.groupValues?.get(1)?.toIntOrNull()
      download.id.takeIf { download.status == DownloadStatus.Downloading && olderBuild != null && olderBuild < build }
    }
  }

  private fun downloadRecords(): List<Download> = downloads.query(DownloadManager.Query())?.use { cursor ->
    val uri = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)
    val id = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
    val status = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
    buildList {
      while (cursor.moveToNext()) {
        val downloadStatus = when (cursor.getInt(status)) {
          DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Ready
          DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
          else -> DownloadStatus.Downloading
        }
        add(Download(cursor.getLong(id), cursor.getString(uri).orEmpty(), downloadStatus))
      }
    }
  }.orEmpty()

  private fun prepareApk(apkUrl: String, manifest: UpdateManifest): File {
    require(manifest.channel == MAINLAND_UPDATE_CHANNEL)
    val download = requireNotNull(findDownload(apkUrl))
    require(download.status == DownloadStatus.Ready)
    val directory = File(app.cacheDir, "app-updates").apply { mkdirs() }
    val apk = File(directory, "Notes-${manifest.build}.apk")
    val descriptor = try {
      downloads.openDownloadedFile(download.id)
    } catch (missing: FileNotFoundException) {
      downloads.remove(download.id)
      throw missing
    }
    try {
      ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
        apk.outputStream().buffered().use { output -> input.copyTo(output) }
      }
      verifyApk(apk, manifest)
    } catch (invalid: IllegalArgumentException) {
      downloads.remove(download.id)
      throw invalid
    }
    return apk
  }

  private fun verifyApk(apk: File, manifest: UpdateManifest) {
    val flags = PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES
    val archive = app.packageManager.getPackageArchiveInfo(apk.path, flags)
    require(archive?.packageName == app.packageName && archive.longVersionCode.toString() == manifest.build)
    require(archive.versionName == manifest.version)
    val region = archive.applicationInfo?.metaData?.getString("me.zhangls.notes.UPDATE_CHANNEL")
    require(publishedAndroidChannel(region) == MAINLAND_UPDATE_CHANNEL)
    val installed = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val expectedSigners = requireNotNull(installed.signingInfo).apkContentsSigners.toSet()
    val actualSigners = requireNotNull(archive.signingInfo).apkContentsSigners.toSet()
    require(expectedSigners.isNotEmpty() && expectedSigners == actualSigners)
  }

  private data class Download(val id: Long, val url: String, val status: DownloadStatus)
}
