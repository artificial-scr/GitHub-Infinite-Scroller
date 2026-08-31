package com.github.infinitescroller.data.repository

import com.github.infinitescroller.data.api.GithubApiService
import com.github.infinitescroller.data.model.SearchResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubRepositoryTest {

    // Minimal stub — only buildQuery is under test, no network calls made
    private val stubApi = object : GithubApiService {
        override suspend fun searchRepositories(
            query: String, sort: String, order: String, perPage: Int, page: Int,
        ) = SearchResponse(0, emptyList())
    }

    private val repo = GithubRepository(stubApi)

    @Test
    fun `empty tags produce fallback query`() {
        assertEquals("stars:>1000", repo.buildQuery(emptySet()))
    }

    @Test
    fun `single tag produces topic query`() {
        assertEquals("topic:android", repo.buildQuery(setOf("android")))
    }

    @Test
    fun `multiple tags are joined with plus separator`() {
        val result = repo.buildQuery(setOf("android", "kotlin"))
        assertTrue(result.contains("topic:android"))
        assertTrue(result.contains("topic:kotlin"))
        assertTrue(result.contains("+"))
    }
}
