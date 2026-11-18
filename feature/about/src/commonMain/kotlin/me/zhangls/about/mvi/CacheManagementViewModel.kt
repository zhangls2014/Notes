package me.zhangls.about.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.zhangls.about.CacheInventory
import me.zhangls.about.CachedFile
import me.zhangls.about.WebAssetCache
import me.zhangls.about.clearUpdateCache
import me.zhangls.about.readCacheInventory
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_clear_complete
import notes.feature.about.generated.resources.about_cache_clear_incomplete
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class CacheManagementViewModel(
  private val cache: WebAssetCache,
  private val toast: ToastGlobalNotifier,
) : ViewModel() {
  private val clearStatus = MutableStateFlow(CacheClearState())
  val transfers = cache.transfers
  val state = combine(
    cache.publicationCount,
    cache.transfers.map { it.keys }.distinctUntilChanged(),
    clearStatus,
  ) { _, _, clear ->
    try {
      CacheManagementState.Loaded(readCacheInventory(), clear.inProgress)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      CacheManagementState.Failed
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, CacheManagementState.Loading)

  fun clear(file: CachedFile) {
    val current = clearStatus.value
    if (current.inProgress) return
    clearStatus.value = current.copy(inProgress = true)
    viewModelScope.launch {
      val cleared = try {
        if (file.site == null) clearUpdateCache(file.relativePath) else cache.clear(file.relativePath)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        false
      }
      clearStatus.value = CacheClearState(revision = current.revision + 1)
      toast.showToast(
        if (cleared) Res.string.about_cache_clear_complete else Res.string.about_cache_clear_incomplete,
      )
    }
  }
}

private data class CacheClearState(val revision: Int = 0, val inProgress: Boolean = false)

internal sealed interface CacheManagementState {
  data object Loading : CacheManagementState
  data object Failed : CacheManagementState
  data class Loaded(val inventory: CacheInventory, val isClearing: Boolean) : CacheManagementState
}
