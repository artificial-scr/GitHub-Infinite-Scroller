package com.github.infinitescroller.data.repository

import java.time.LocalDate

enum class SortOption(val label: String, val apiSort: String) {
    STARS("Most stars", "stars"),
    RISING("Rising this month", "stars"),
    UPDATED("Recently updated", "updated"),
    FORKS("Most forks", "forks"),
}

/**
 * What the feed should show.
 * @param topic a single GitHub topic, or null for the "Popular" feed.
 * @param text free-text search terms entered by the user.
 */
data class FeedRequest(
    val topic: String? = null,
    val sort: SortOption = SortOption.STARS,
    val text: String = "",
)

object QueryBuilder {

    const val RISING_WINDOW_DAYS = 30L
    private const val POPULAR_FLOOR = "stars:>1000"

    /**
     * Builds the `q` parameter. Qualifiers are space-separated (an implicit AND); GitHub does
     * not allow `OR` between qualifiers, which is why a request carries at most one topic.
     */
    fun build(request: FeedRequest, today: LocalDate): String {
        val parts = mutableListOf<String>()
        request.text.trim().replace(Regex("\\s+"), " ").takeIf { it.isNotEmpty() }?.let(parts::add)
        request.topic?.let { parts += "topic:$it" }
        if (request.sort == SortOption.RISING) {
            parts += "created:>=${today.minusDays(RISING_WINDOW_DAYS)}"
        }
        if (parts.isEmpty()) parts += POPULAR_FLOOR
        parts += "archived:false"
        return parts.joinToString(" ")
    }
}
