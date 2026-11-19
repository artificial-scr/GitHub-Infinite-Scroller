package com.github.infinitescroller.ui.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

object Format {

    /** GitHub-style compact counts: 999, 1.2k, 12.3k, 123k, 1.2M. */
    fun compactCount(value: Int): String {
        val n = value.coerceAtLeast(0)
        return when {
            n < 1_000 -> n.toString()
            n < 100_000 -> trimmed(n / 1_000.0) + "k"
            n < 1_000_000 -> "${n / 1_000}k"
            else -> trimmed(n / 1_000_000.0) + "M"
        }
    }

    /** "just now", "5m ago", "3h ago", "2d ago", "3w ago", "4mo ago", "2y ago". */
    fun relativeTime(iso: String?, now: Instant = Instant.now()): String? {
        val then = parse(iso) ?: return null
        val seconds = Duration.between(then, now).seconds.coerceAtLeast(0)
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "just now"
            hours < 1 -> "${minutes}m ago"
            days < 1 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    }

    /** Localised medium date, e.g. "Mar 4, 2024". */
    fun date(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String? =
        parse(iso)?.atZone(zone)?.toLocalDate()
            ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** Whole seconds until [epochSeconds], never negative. */
    fun secondsUntil(epochSeconds: Long, now: Instant = Instant.now()): Long =
        (epochSeconds - now.epochSecond).coerceAtLeast(0)

    private fun parse(iso: String?): Instant? = try {
        iso?.let(Instant::parse)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun trimmed(value: Double): String {
        // Truncate rather than round so 999,999 never shows as "1000k".
        val truncated = kotlin.math.floor(value * 10) / 10
        val text = String.format(Locale.US, "%.1f", truncated)
        return text.removeSuffix(".0")
    }
}
