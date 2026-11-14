package me.zhangls.about.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
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
import me.zhangls.about.loadReleaseHistory
import me.zhangls.about.publishedAndroidChannel
import me.zhangls.about.toAboutAppInfo
import me.zhangls.framework.AppInfo
import me.zhangls.framework.mvi.MviViewModel
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
internal class AboutViewModel(
  savedStateHandle: SavedStateHandle,
  private val appInfo: AppInfo,
  private val site: NotesWebsite,
  @Named("publicUpdates") private val updatesClient: HttpClient,
  private val installer: UpdateInstaller,
) :
  MviViewModel<AboutState, AboutIntent>(
    initialState = AboutState(appInfo.toAboutAppInfo()),
    stateSerializer = AboutState.serializer(),
    savedStateHandle = savedStateHandle,
  ) {
  // Keep loaded content across navigation so a temporary loading height cannot clamp the restored scroll offset.
  val releases = flow { emit(loadReleaseHistory()) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val updateChannel = publishedAndroidChannel(appInfo.getUpdateChannel())
  val canCheckForUpdates = updateChannel != null
  private var updateJob: Job? = null
  private var downloadObserver: Job? = null
  private val mutableUpdateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
  val updateStatus = mutableUpdateStatus.asStateFlow()

  private fun checkForUpdates() {
    val channel = updateChannel?.takeUnless { updateJob?.isActive == true } ?: return
    downloadObserver?.cancel()
    mutableUpdateStatus.value = UpdateStatus.Checking
    updateJob = viewModelScope.launch {
      mutableUpdateStatus.value = try {
        val manifest = checkAvailableUpdate(updatesClient, appInfo, channel, site)
        if (manifest == null) {
          UpdateStatus.Current
        } else {
          val release = loadPublishedUpdateNotes(updatesClient, site, manifest.version)
          val apkUrl = mainlandApkUrl(manifest)
          val download = apkUrl?.let { installer.observeDownload(it).first() } ?: DownloadStatus.Idle
          UpdateStatus.Available(
            manifest,
            release,
            apkUrl,
            download,
            showDownloadStatus = true,
            olderDownloadActive = apkUrl?.let { installer.hasOlderDownload(it) } == true,
          )
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: UpdateCheckException) {
        UpdateStatus.Failed(failure.reason, failure.status)
      }
      val available = mutableUpdateStatus.value as? UpdateStatus.Available
      available?.let { observeDownload(it) }
    }
  }

  private fun observeDownload(available: UpdateStatus.Available) {
    val url = available.apkUrl ?: return
    downloadObserver?.cancel()
    downloadObserver = viewModelScope.launch {
      installer.observeDownload(url).collect { download ->
        val current = mutableUpdateStatus.value as? UpdateStatus.Available
        if (current?.manifest == available.manifest) {
          mutableUpdateStatus.value = current.copy(download = download)
        }
      }
    }
  }

  private fun updateAction() {
    val available = (mutableUpdateStatus.value as? UpdateStatus.Available)
      ?.takeUnless { it.working || it.download == DownloadStatus.Downloading } ?: return
    val url = available.apkUrl ?: return
    downloadObserver?.cancel()
    mutableUpdateStatus.value = available.copy(working = true, installResult = null)
    updateJob = viewModelScope.launch {
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

  private fun dismissUpdateResult() {
    val current = mutableUpdateStatus.value as? UpdateStatus.Available
    if (current?.download == DownloadStatus.Downloading) {
      mutableUpdateStatus.value = current.copy(showDownloadStatus = false)
    } else {
      downloadObserver?.cancel()
      updateJob?.cancel()
      mutableUpdateStatus.value = UpdateStatus.Idle
    }
  }

  override fun handleIntent(intent: AboutIntent) {
    when (intent) {
      AboutIntent.CheckForUpdates -> checkForUpdates()

      AboutIntent.UpdateAction -> updateAction()

      AboutIntent.DismissUpdateResult -> dismissUpdateResult()

      AboutIntent.ToggleVersionInfo -> updateState {
        AboutReducer.reduce(this, AboutAction.SetVersionInfoExpanded(!isVersionInfoExpanded))
      }
    }
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
