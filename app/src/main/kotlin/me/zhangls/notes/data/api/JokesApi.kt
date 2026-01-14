package me.zhangls.notes.data.api

import me.zhangls.network.ApiResponse
import me.zhangls.notes.data.response.JokePageResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author zhangls
 */
interface JokesApi {
  @GET("api/jokes/list")
  suspend fun jokes(
    @Query("app_id") appId: String,
    @Query("app_secret") appSecret: String,
    @Query("page") page: Int
  ): ApiResponse<JokePageResponse>
}