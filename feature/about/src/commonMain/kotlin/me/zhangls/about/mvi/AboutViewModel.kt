package me.zhangls.about.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import me.zhangls.about.loadReleaseHistory
import me.zhangls.about.toAboutAppInfo
import me.zhangls.framework.AppInfo
import me.zhangls.framework.mvi.MviViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AboutViewModel(savedStateHandle: SavedStateHandle, appInfo: AppInfo) :
  MviViewModel<AboutState, AboutIntent>(
    initialState = AboutState(appInfo.toAboutAppInfo()),
    stateSerializer = AboutState.serializer(),
    savedStateHandle = savedStateHandle,
  ) {
  // Keep loaded content across navigation so a temporary loading height cannot clamp the restored scroll offset.
  val releases = flow { emit(loadReleaseHistory()) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  override fun handleIntent(intent: AboutIntent) {
    when (intent) {
      AboutIntent.ToggleVersionInfo -> updateState {
        AboutReducer.reduce(this, AboutAction.SetVersionInfoExpanded(!isVersionInfoExpanded))
      }
    }
  }
}
