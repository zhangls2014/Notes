package me.zhangls.notes.data.repository

import me.zhangls.network.NetworkResult
import me.zhangls.network.safeApiCall
import me.zhangls.notes.data.api.JokesApi
import me.zhangls.notes.data.response.JokePageResponse
import org.koin.core.annotation.Factory

/**
 * @author zhangls
 */
@Factory
class JokesRepository(private val jokesApi: JokesApi) {
  suspend fun getJokes(appId: String, appSecret: String, page: Int): NetworkResult<JokePageResponse> = safeApiCall {
    jokesApi.jokes(appId, appSecret, page)
  }
}