package com.github.infinitescroller.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.infinitescroller.data.api.GithubApi
import com.github.infinitescroller.data.api.GithubErrorParser
import com.github.infinitescroller.data.model.Repo
import kotlinx.coroutines.CancellationException

class SearchPagingSource(
    private val api: GithubApi,
    private val errorParser: GithubErrorParser,
    private val query: String,
    private val sort: String,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) : PagingSource<Int, Repo>() {

    // Search rankings can shift between page requests, so the same repo may appear on two pages.
    // Duplicate keys crash LazyColumn, so drop anything this generation has already emitted.
    private val seenIds = HashSet<Long>()

    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val response = api.searchRepositories(
                query = query,
                sort = sort,
                perPage = PAGE_SIZE,
                page = page,
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return LoadResult.Error(
                    errorParser.parse(
                        code = response.code(),
                        header = { response.headers()[it] },
                        body = response.errorBody()?.string(),
                        nowEpochSeconds = clock(),
                    ),
                )
            }

            val reachable = minOf(body.totalCount, MAX_RESULTS)
            val nextKey = if (body.items.isEmpty() || page * PAGE_SIZE >= reachable) null else page + 1
            val fresh = synchronized(seenIds) { body.items.filter { seenIds.add(it.id) } }

            LoadResult.Page(
                data = fresh,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 30

        /** GitHub's search API never returns more than 1,000 results per query. */
        const val MAX_RESULTS = 1000
    }
}
