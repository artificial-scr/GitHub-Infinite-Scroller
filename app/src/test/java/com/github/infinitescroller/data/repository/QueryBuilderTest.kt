package com.github.infinitescroller.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class QueryBuilderTest {

    private val today = LocalDate.of(2026, 11, 19)

    private fun build(topic: String? = null, sort: SortOption = SortOption.STARS, text: String = "") =
        QueryBuilder.build(FeedRequest(topic, sort, text), today)

    @Test
    fun `popular feed uses a star floor`() {
        assertEquals("stars:>1000 archived:false", build())
    }

    @Test
    fun `topic feed queries exactly one topic`() {
        assertEquals("topic:android archived:false", build(topic = "android"))
    }

    @Test
    fun `query never joins qualifiers with OR`() {
        // GitHub answers HTTP 422 to `topic:a OR topic:b`; this was the v1.0 crash.
        SortOption.entries.forEach { sort ->
            listOf(null, "android").forEach { topic ->
                listOf("", "compose ui").forEach { text ->
                    val q = build(topic, sort, text)
                    assertFalse(q, Regex("\\b(OR|AND|NOT)\\b").containsMatchIn(q))
                }
            }
        }
    }

    @Test
    fun `search text is trimmed, collapsed and combined with the topic`() {
        assertEquals("image loader topic:kotlin archived:false", build(topic = "kotlin", text = "  image   loader "))
    }

    @Test
    fun `search text alone drops the star floor`() {
        assertEquals("compose archived:false", build(text = "compose"))
    }

    @Test
    fun `rising adds a 30 day creation window`() {
        assertEquals("topic:rust created:>=2026-10-20 archived:false", build(topic = "rust", sort = SortOption.RISING))
        assertEquals("created:>=2026-10-20 archived:false", build(sort = SortOption.RISING))
    }

    @Test
    fun `sort options map to GitHub sort keys`() {
        assertEquals("stars", SortOption.STARS.apiSort)
        assertEquals("stars", SortOption.RISING.apiSort)
        assertEquals("updated", SortOption.UPDATED.apiSort)
        assertEquals("forks", SortOption.FORKS.apiSort)
    }
}
