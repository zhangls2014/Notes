package me.zhangls.about.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.zhangls.about.CacheInventory
import me.zhangls.about.WebAssetCache
import me.zhangls.about.readCacheInventory
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class CacheManagementViewModel(cache: WebAssetCache) : ViewModel() {
  val transfers = cache.transfers
  val state = combine(
    cache.publicationCount,
    cache.transfers.map { it.keys }.distinctUntilChanged(),
  ) { _, _ ->
    try {
      CacheManagementState.Loaded(readCacheInventory())
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      CacheManagementState.Failed
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, CacheManagementState.Loading)
}

internal sealed interface CacheManagementState {
  data object Loading : CacheManagementState
  data object Failed : CacheManagementState
  data class Loaded(val inventory: CacheInventory) : CacheManagementState
}
