package me.zhangls.about.mvi

import androidx.lifecycle.SavedStateHandle
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.framework.mvi.MviViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AboutViewModel(
  savedStateHandle: SavedStateHandle,
  @InjectedParam appInfo: AboutAppInfo,
) : MviViewModel<AboutState, AboutIntent>(
  initialState = AboutState(appInfo),
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
