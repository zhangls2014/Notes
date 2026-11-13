package me.zhangls.about.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.UpdateManifest
import me.zhangls.about.UpdateCheckException
import me.zhangls.about.UpdateFailure
import me.zhangls.about.checkAvailableUpdate
import me.zhangls.about.loadReleaseHistory
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
) :
  MviViewModel<AboutState, AboutIntent>(
    initialState = AboutState(appInfo.toAboutAppInfo()),
    stateSerializer = AboutState.serializer(),
    savedStateHandle = savedStateHandle,
  ) {
  // Keep loaded content across navigation so a temporary loading height cannot clamp the restored scroll offset.
  val releases = flow { emit(loadReleaseHistory()) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val updateChannel = appInfo.getUpdateChannel()
  private var updateJob: Job? = null
  private val mutableUpdateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
  val updateStatus = mutableUpdateStatus.asStateFlow()

  private fun checkForUpdates() {
    val channel = updateChannel ?: return
    if (mutableUpdateStatus.value == UpdateStatus.Checking) return
    mutableUpdateStatus.value = UpdateStatus.Checking
    updateJob = viewModelScope.launch {
      mutableUpdateStatus.value = try {
        checkAvailableUpdate(updatesClient, appInfo, channel, site)?.let(UpdateStatus::Available) ?: UpdateStatus.Current
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: UpdateCheckException) {
        UpdateStatus.Failed(failure.reason, failure.status)
      }
    }
  }

  override fun handleIntent(intent: AboutIntent) {
    when (intent) {
      AboutIntent.CheckForUpdates -> checkForUpdates()

      AboutIntent.DismissUpdateResult -> {
        updateJob?.cancel()
        mutableUpdateStatus.value = UpdateStatus.Idle
      }

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
  data class Available(val manifest: UpdateManifest) : UpdateStatus
}
