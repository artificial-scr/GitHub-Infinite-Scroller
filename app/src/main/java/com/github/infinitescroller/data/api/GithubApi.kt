package com.github.infinitescroller.data.api

import com.github.infinitescroller.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubApi {

    /**
     * Note: GitHub rejects `OR` between qualifiers (e.g. `topic:a OR topic:b`) with HTTP 422,
     * so callers must send one topic per query. See [com.github.infinitescroller.data.repository.QueryBuilder].
     */
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String,
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int,
        @Query("page") page: Int,
    ): Response<SearchResponse>
}
