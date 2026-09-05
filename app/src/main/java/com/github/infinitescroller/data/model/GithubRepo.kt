package com.github.infinitescroller.data.model

import com.squareup.moshi.Json

data class GithubRepo(
    val id: Long,
    @Json(name = "full_name") val fullName: String,
    val name: String,
    val description: String?,
    @Json(name = "stargazers_count") val stars: Int,
    @Json(name = "forks_count") val forks: Int,
    @Json(name = "pushed_at") val pushedAt: String?,
    val language: String?,
    val topics: List<String> = emptyList(),
    @Json(name = "html_url") val htmlUrl: String,
    val owner: Owner,
)

data class Owner(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
)

data class SearchResponse(
    @Json(name = "total_count") val totalCount: Int,
    val items: List<GithubRepo>,
)
