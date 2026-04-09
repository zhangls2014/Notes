package me.zhangls.email.search

import androidx.compose.material3.ExperimentalMaterial3Api


internal object SearchReducer {
  @OptIn(ExperimentalMaterial3Api::class)
  fun reduce(oldState: SearchState, action: SearchAction): SearchState {
    return with(oldState) {
      when (action) {
        is SearchAction.UpdateUser -> copy(user = action.user)
        is SearchAction.UpdateSearchHistory -> copy(searchHistory = action.history)
        is SearchAction.UpdateSearchText -> copy(searchText = action.text)
        is SearchAction.UpdateSearchBarValue -> copy(searchBarValue = action.value)
      }
    }
  }
}
