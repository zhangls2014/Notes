package me.zhangls.notes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.network.NetworkResult
import me.zhangls.notes.data.repository.JokesRepository
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class JokeViewModel @Inject constructor(
  private val jokesRepository: JokesRepository,
  private val userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
): ViewModel() {
  private val appId = ""
  private val appSecret = ""


  init {
    viewModelScope.launch {
      userRepository.userFlow.collectLatest {
        println("user ==> $it")
      }
    }
    viewModelScope.launch {
      settingsRepository.settingsFlow.collectLatest {
        println("settings ==> $it")
      }
    }
  }

  fun getJokes(page: Int) {
    viewModelScope.launch {
      when (val result = jokesRepository.getJokes(appId, appSecret, page)) {
        is NetworkResult.Failure -> println("getJokes failure ==> ${result.error}")
        is NetworkResult.Success -> println("getJokes success")
      }
    }
  }
}