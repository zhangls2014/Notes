package me.zhangls.about.mvi

internal object AboutReducer {
  fun reduce(state: AboutState, action: AboutAction): AboutState = when (action) {
    is AboutAction.SetBuildInfoExpanded -> state.copy(isBuildInfoExpanded = action.expanded)
  }
}
