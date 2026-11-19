package com.github.infinitescroller.ui.util

import com.github.infinitescroller.data.api.GithubException
import java.io.IOException
import java.time.Instant

data class ErrorMessage(val title: String, val body: String)

fun Throwable.toErrorMessage(hasToken: Boolean, now: Instant = Instant.now()): ErrorMessage = when (this) {
    is GithubException.RateLimited -> {
        val wait = retryAtEpochSeconds?.let { Format.secondsUntil(it, now) }
        val whenText = when {
            wait == null -> "Try again in a minute."
            wait < 60 -> "Try again in ${wait.coerceAtLeast(1)}s."
            else -> "Try again in ${(wait + 59) / 60} min."
        }
        val hint = if (hasToken) "" else " Add a GitHub token in Settings to raise the limit."
        ErrorMessage("Slow down a little", "GitHub's search limit was reached. $whenText$hint")
    }
    is GithubException.Unauthorized -> ErrorMessage(
        "Token rejected",
        "GitHub didn't accept your access token. Update or remove it in Settings.",
    )
    is GithubException.InvalidQuery -> ErrorMessage(
        "GitHub couldn't run this search",
        detail,
    )
    is GithubException.Http -> ErrorMessage(
        "GitHub is having trouble",
        "The server answered with error $code. Please try again shortly.",
    )
    is IOException -> ErrorMessage(
        "You're offline",
        "Check your internet connection and try again.",
    )
    else -> ErrorMessage("Something went wrong", message ?: "An unexpected error occurred.")
}
