package com.github.infinitescroller.data.model

import com.squareup.moshi.Json

/**
 * A GitHub repository as returned by the search API. The same model is persisted
 * (as JSON) for bookmarks, so every field that may be absent has a default.
 */
data class Repo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val description: String? = null,
    @Json(name = "html_url") val htmlUrl: String,
    val homepage: String? = null,
    @Json(name = "stargazers_count") val stars: Int = 0,
    @Json(name = "forks_count") val forks: Int = 0,
    @Json(name = "open_issues_count") val openIssues: Int = 0,
    val language: String? = null,
    val topics: List<String> = emptyList(),
    val license: License? = null,
    val owner: Owner,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "pushed_at") val pushedAt: String? = null,
    val archived: Boolean = false,
)

data class Owner(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String,
)

data class License(
    @Json(name = "spdx_id") val spdxId: String? = null,
    val name: String? = null,
) {
    /** Short label such as "MIT"; GitHub uses "NOASSERTION" for unrecognised licenses. */
    val label: String?
        get() = spdxId?.takeUnless { it.isBlank() || it == "NOASSERTION" } ?: name?.takeUnless { it.isBlank() }
}

data class SearchResponse(
    @Json(name = "total_count") val totalCount: Int,
    val items: List<Repo>,
)

/** Error body GitHub sends with 4xx responses. */
data class GithubErrorBody(
    val message: String? = null,
    val errors: List<GithubErrorItem>? = null,
)

data class GithubErrorItem(
    val message: String? = null,
)
