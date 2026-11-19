package com.github.infinitescroller.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.github.infinitescroller.data.model.Repo

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to open links", Toast.LENGTH_SHORT).show()
    }
}

fun Context.shareRepo(repo: Repo) {
    val text = buildString {
        append(repo.fullName)
        repo.description?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
        append('\n').append(repo.htmlUrl)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, repo.fullName)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, "Share repository"))
}

/** Homepage values from GitHub are free text; only treat real web links as links. */
fun String?.asWebUrl(): String? {
    val value = this?.trim().orEmpty()
    if (value.isEmpty()) return null
    return when {
        value.startsWith("https://") || value.startsWith("http://") -> value
        value.contains('.') && !value.contains(' ') -> "https://$value"
        else -> null
    }
}
