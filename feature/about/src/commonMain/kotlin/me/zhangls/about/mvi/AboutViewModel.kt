package me.zhangls.about.mvi

import androidx.lifecycle.SavedStateHandle
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
  override fun handleIntent(intent: AboutIntent) {
    when (intent) {
      AboutIntent.ToggleBuildInfo -> updateState {
        AboutReducer.reduce(this, AboutAction.SetBuildInfoExpanded(!isBuildInfoExpanded))
      }
    }
  }
}
