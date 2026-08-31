package com.github.infinitescroller.data.paging

import androidx.paging.PagingSource
import com.github.infinitescroller.data.api.GithubApiService
import com.github.infinitescroller.data.model.GithubRepo
import com.github.infinitescroller.data.model.Owner
import com.github.infinitescroller.data.model.SearchResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GithubPagingSourceTest {

    private val fakeRepos = (1..20).map { i ->
        GithubRepo(
            id = i.toLong(),
            fullName = "owner/repo$i",
            name = "repo$i",
            description = "Description $i",
            stars = i * 100,
            forks = i * 10,
            pushedAt = "2024-01-01T00:00:00Z",
            language = "Kotlin",
            topics = listOf("android"),
            htmlUrl = "https://github.com/owner/repo$i",
            owner = Owner("owner", "https://example.com/avatar.png"),
        )
    }

    private fun apiReturning(repos: List<GithubRepo>) = object : GithubApiService {
        override suspend fun searchRepositories(
            query: String, sort: String, order: String, perPage: Int, page: Int,
        ) = SearchResponse(totalCount = repos.size, items = repos)
    }

    private val throwingApi = object : GithubApiService {
        override suspend fun searchRepositories(
            query: String, sort: String, order: String, perPage: Int, page: Int,
        ): SearchResponse = throw IOException("Network error")
    }

    private fun refreshParams(page: Int? = null) =
        PagingSource.LoadParams.Refresh(key = page, loadSize = 20, placeholdersEnabled = false)

    @Test
    fun `first page has null prevKey and nextKey 2`() = runTest {
        val source = GithubPagingSource(apiReturning(fakeRepos), "topic:android")
        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
        assertEquals(fakeRepos, result.data)
    }

    @Test
    fun `subsequent page has correct prevKey and nextKey`() = runTest {
        val source = GithubPagingSource(apiReturning(fakeRepos), "topic:android")
        val result = source.load(refreshParams(page = 3)) as PagingSource.LoadResult.Page

        assertEquals(2, result.prevKey)
        assertEquals(4, result.nextKey)
    }

    @Test
    fun `empty result sets nextKey to null`() = runTest {
        val source = GithubPagingSource(apiReturning(emptyList()), "topic:android")
        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `API error produces LoadResult Error`() = runTest {
        val source = GithubPagingSource(throwingApi, "topic:android")
        val result = source.load(refreshParams())

        assertTrue(result is PagingSource.LoadResult.Error)
        assertTrue((result as PagingSource.LoadResult.Error).throwable is IOException)
    }
}
