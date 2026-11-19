package me.zhangls.about.mvi

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhangls.about.AppRelease
import me.zhangls.about.DownloadStatus
import me.zhangls.about.InstallResult
import me.zhangls.about.MAINLAND_UPDATE_CHANNEL
import me.zhangls.about.UpdateCheckException
import me.zhangls.about.UpdateFailure
import me.zhangls.about.UpdateInstaller
import me.zhangls.about.UpdateManifest
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.checkAvailableUpdate
import me.zhangls.about.loadPublishedUpdateNotes
import me.zhangls.about.publishedAndroidChannel
import me.zhangls.framework.AppInfo

internal class UpdateController(
  private val appInfo: AppInfo,
  private val site: NotesWebsite,
  private val updatesClient: HttpClient,
  private val installer: UpdateInstaller,
  private val scope: CoroutineScope,
) {
  private var foreground = false
  private var manualCheckRequested = false
  private val dismissedBuilds = mutableSetOf<Pair<String, String>>()

  fun onForegroundChanged(isForeground: Boolean) {
    if (foreground == isForeground) return
    foreground = isForeground
    if (isForeground) checkForUpdates(automatic = true)
  }

  private val updateChannel = publishedAndroidChannel(appInfo.getUpdateChannel())
  val canCheckForUpdates = updateChannel != null
  private var updateJob: Job? = null
  private var downloadObserver: Job? = null
  private val mutableUpdateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
  val updateStatus = mutableUpdateStatus.asStateFlow()

  fun checkForUpdates(automatic: Boolean = false) {
    if (!automatic) manualCheckRequested = true
    val channel = updateChannel?.takeUnless { updateJob?.isActive == true } ?: return
    val previous = mutableUpdateStatus.value
    val activeDownload = previous as? UpdateStatus.Available
    if (!automatic) {
      downloadObserver?.cancel()
      mutableUpdateStatus.value = UpdateStatus.Checking
    }
    manualCheckRequested = !automatic
    updateJob = scope.launch {
      mutableUpdateStatus.value = try {
        val manifest = checkAvailableUpdate(updatesClient, appInfo, channel, site)
        if (manifest == null) {
          if (manualCheckRequested) UpdateStatus.Current else UpdateStatus.Idle
        } else {
          if (!manualCheckRequested && manifest.channel to manifest.build in dismissedBuilds) {
            return@launch
          }
          if (!manualCheckRequested && activeDownload?.manifest == manifest) return@launch
          availableStatus(manifest)
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: UpdateCheckException) {
        if (manualCheckRequested) UpdateStatus.Failed(failure.reason, failure.status) else previous
      }
      val available = mutableUpdateStatus.value as? UpdateStatus.Available
      available?.let { observeDownload(it) }
    }
  }

  private suspend fun availableStatus(manifest: UpdateManifest): UpdateStatus.Available {
    val release = loadPublishedUpdateNotes(updatesClient, site, manifest.version)
    val apkUrl = mainlandApkUrl(manifest)
    val download = apkUrl?.let { installer.observeDownload(it).first() } ?: DownloadStatus.Idle
    return UpdateStatus.Available(
      manifest,
      release,
      apkUrl,
      download,
      showDownloadStatus = true,
      olderDownloadActive = apkUrl?.let { installer.hasOlderDownload(it) } == true,
    )
  }

  private fun observeDownload(available: UpdateStatus.Available) {
    val url = available.apkUrl ?: return
    downloadObserver?.cancel()
    downloadObserver = scope.launch {
      installer.observeDownload(url).collect { download ->
        val current = mutableUpdateStatus.value as? UpdateStatus.Available
        if (current?.manifest == available.manifest) {
          mutableUpdateStatus.value = current.copy(download = download)
        }
      }
    }
  }

  fun updateAction() {
    val available = (mutableUpdateStatus.value as? UpdateStatus.Available)
      ?.takeUnless { it.working || it.download == DownloadStatus.Downloading } ?: return
    val url = available.apkUrl ?: return
    updateJob?.cancel()
    downloadObserver?.cancel()
    mutableUpdateStatus.value = available.copy(working = true, installResult = null)
    updateJob = scope.launch {
      if (available.download == DownloadStatus.Ready) {
        val result = installer.install(url, available.manifest)
        val settled = available.copy(installResult = result)
        mutableUpdateStatus.value = if (result == InstallResult.HandedOff) UpdateStatus.Idle else settled
        if (result == InstallResult.Failed) observeDownload(settled)
      } else if (installer.enqueue(url)) {
        val downloading = available.copy(
          download = DownloadStatus.Downloading,
          installResult = null,
          showDownloadStatus = false,
          olderDownloadActive = false,
        )
        mutableUpdateStatus.value = downloading
        observeDownload(downloading)
      } else {
        mutableUpdateStatus.value = available.copy(download = DownloadStatus.Failed)
      }
    }
  }

  private fun mainlandApkUrl(manifest: UpdateManifest): String? {
    val origin = site.apkOrigin?.takeIf { manifest.channel == MAINLAND_UPDATE_CHANNEL } ?: return null
    return "$origin/${manifest.channel}/${manifest.build}/Notes-${manifest.version}-mainland.apk"
  }

  fun dismissUpdateResult() {
    val current = mutableUpdateStatus.value as? UpdateStatus.Available
    current?.let { dismissedBuilds += it.manifest.channel to it.manifest.build }
    downloadObserver?.cancel()
    updateJob?.cancel()
    mutableUpdateStatus.value = UpdateStatus.Idle
  }
}

internal sealed interface UpdateStatus {
  data object Idle : UpdateStatus
  data object Checking : UpdateStatus
  data object Current : UpdateStatus
  data class Failed(val reason: UpdateFailure, val status: Int?) : UpdateStatus
  data class Available(
    val manifest: UpdateManifest,
    val release: AppRelease?,
    val apkUrl: String?,
    val download: DownloadStatus = DownloadStatus.Idle,
    val working: Boolean = false,
    val installResult: InstallResult? = null,
    val showDownloadStatus: Boolean = false,
    val olderDownloadActive: Boolean = false,
  ) : UpdateStatus
}
