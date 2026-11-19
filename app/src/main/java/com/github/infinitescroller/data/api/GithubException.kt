package com.github.infinitescroller.data.api

import com.github.infinitescroller.data.model.GithubErrorBody
import com.squareup.moshi.Moshi

/** Typed failures from the GitHub API, so the UI can show a precise message. */
sealed class GithubException(message: String) : Exception(message) {

    /** Primary or secondary rate limit hit. [retryAtEpochSeconds] is null when GitHub gave no hint. */
    class RateLimited(val retryAtEpochSeconds: Long?) : GithubException("GitHub rate limit exceeded")

    /** The saved personal access token was rejected. */
    class Unauthorized : GithubException("GitHub rejected the access token")

    /** HTTP 422: GitHub could not process the search query. */
    class InvalidQuery(val detail: String) : GithubException(detail)

    class Http(val code: Int, val detail: String) : GithubException("HTTP $code: $detail")
}

class GithubErrorParser(moshi: Moshi) {

    private val adapter = moshi.adapter(GithubErrorBody::class.java)

    /**
     * @param header looks up a response header by name (case-insensitive).
     */
    fun parse(
        code: Int,
        header: (String) -> String?,
        body: String?,
        nowEpochSeconds: Long,
    ): GithubException {
        val parsed = body?.takeIf { it.isNotBlank() }?.let {
            runCatching { adapter.fromJson(it) }.getOrNull()
        }
        val message = parsed?.message.orEmpty()
        val detail = parsed?.errors?.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) }
            ?: message.ifBlank { "Unexpected response from GitHub" }

        val remaining = header("x-ratelimit-remaining")?.toIntOrNull()
        val reset = header("x-ratelimit-reset")?.toLongOrNull()
        val retryAfter = header("retry-after")?.toLongOrNull()
        val looksRateLimited = remaining == 0 || retryAfter != null ||
            message.contains("rate limit", ignoreCase = true)

        return when {
            code == 401 -> GithubException.Unauthorized()
            (code == 403 || code == 429) && looksRateLimited -> GithubException.RateLimited(
                retryAtEpochSeconds = retryAfter?.let { nowEpochSeconds + it } ?: reset,
            )
            code == 422 -> GithubException.InvalidQuery(detail)
            else -> GithubException.Http(code, detail)
        }
    }
}
