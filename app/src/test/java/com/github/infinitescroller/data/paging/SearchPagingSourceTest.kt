package com.github.infinitescroller.data.paging

import androidx.paging.PagingSource
import com.github.infinitescroller.data.api.GithubErrorParser
import com.github.infinitescroller.data.api.GithubException
import com.github.infinitescroller.data.model.SearchResponse
import com.github.infinitescroller.testing.FakeGithubApi
import com.github.infinitescroller.testing.errorResponse
import com.github.infinitescroller.testing.repo
import com.github.infinitescroller.testing.testMoshi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SearchPagingSourceTest {

    private val pageSize = SearchPagingSource.PAGE_SIZE
    private val parser = GithubErrorParser(testMoshi)

    private fun source(api: FakeGithubApi) =
        SearchPagingSource(api, parser, query = "topic:android archived:false", sort = "stars", clock = { 1_000L })

    private fun page(ids: LongRange, total: Int) =
        Response.success(SearchResponse(total, ids.map { repo(it) }))

    private fun refresh() = PagingSource.LoadParams.Refresh<Int>(null, pageSize, false)
    private fun append(key: Int) = PagingSource.LoadParams.Append(key, pageSize, false)

    @Test
    fun `first page requests page 1 with a fixed page size`() = runTest {
        val api = FakeGithubApi { _, _ -> page(1L..30L, total = 500) }
        val result = source(api).load(refresh()) as PagingSource.LoadResult.Page

        assertEquals(FakeGithubApi.Call("topic:android archived:false", "stars", pageSize, 1), api.calls.single())
        assertEquals(30, result.data.size)
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
    }

    @Test
    fun `stops when total count is reached`() = runTest {
        val api = FakeGithubApi { _, _ -> page(1L..15L, total = 45) }
        val result = source(api).load(append(2)) as PagingSource.LoadResult.Page
        assertNull(result.nextKey)
    }

    @Test
    fun `stops at GitHub's 1000 result cap`() = runTest {
        val lastPage = SearchPagingSource.MAX_RESULTS / pageSize + 1 // page 34 reaches 1,020
        val api = FakeGithubApi { _, _ -> page(1L..30L, total = 250_000) }
        val result = source(api).load(append(lastPage)) as PagingSource.LoadResult.Page
        assertNull(result.nextKey)

        val before = source(api).load(append(lastPage - 1)) as PagingSource.LoadResult.Page
        assertEquals(lastPage, before.nextKey)
    }

    @Test
    fun `empty page ends pagination`() = runTest {
        val api = FakeGithubApi { _, _ -> page(LongRange.EMPTY, total = 0) }
        val result = source(api).load(refresh()) as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
        assertNull(result.nextKey)
    }

    @Test
    fun `repos repeated across pages are dropped`() = runTest {
        val api = FakeGithubApi { _, p -> if (p == 1) page(1L..30L, 900) else page(25L..54L, 900) }
        val src = source(api)
        src.load(refresh())
        val second = src.load(append(2)) as PagingSource.LoadResult.Page
        assertEquals((31L..54L).toList(), second.data.map { it.id })
    }

    @Test
    fun `422 becomes InvalidQuery with GitHub's explanation`() = runTest {
        val body = """{"message":"Validation Failed","errors":[{"message":"Logical operators only apply to text"}]}"""
        val api = FakeGithubApi { _, _ -> errorResponse(422, body) }
        val error = (source(api).load(refresh()) as PagingSource.LoadResult.Error).throwable
        assertTrue(error is GithubException.InvalidQuery)
        assertEquals("Logical operators only apply to text", (error as GithubException.InvalidQuery).detail)
    }

    @Test
    fun `403 with exhausted quota becomes RateLimited`() = runTest {
        val api = FakeGithubApi { _, _ ->
            errorResponse(
                403,
                """{"message":"API rate limit exceeded"}""",
                mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1060"),
            )
        }
        val error = (source(api).load(refresh()) as PagingSource.LoadResult.Error).throwable
        assertEquals(1060L, (error as GithubException.RateLimited).retryAtEpochSeconds)
    }

    @Test
    fun `network failure is reported, not thrown`() = runTest {
        val api = FakeGithubApi { _, _ -> throw IOException("offline") }
        val result = source(api).load(refresh())
        assertTrue((result as PagingSource.LoadResult.Error).throwable is IOException)
    }
}
