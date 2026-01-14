package me.zhangls.notes.data.repository

import me.zhangls.network.NetworkResult
import me.zhangls.network.safeApiCall
import me.zhangls.notes.data.response.JokePageResponse
import me.zhangls.notes.data.api.JokesApi
import javax.inject.Inject

/**
 * @author zhangls
 */
class JokesRepository @Inject constructor(private val jokesApi: JokesApi) {
  suspend fun getJokes(appId: String, appSecret: String, page: Int): NetworkResult<JokePageResponse> = safeApiCall {
    jokesApi.jokes(appId, appSecret, page)
  }
}