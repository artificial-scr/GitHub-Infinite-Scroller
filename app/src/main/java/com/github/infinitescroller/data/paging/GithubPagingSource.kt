package com.github.infinitescroller.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.infinitescroller.data.api.GithubApiService
import com.github.infinitescroller.data.model.GithubRepo
import kotlinx.coroutines.CancellationException

class GithubPagingSource(
    private val api: GithubApiService,
    private val query: String,
) : PagingSource<Int, GithubRepo>() {

    override fun getRefreshKey(state: PagingState<Int, GithubRepo>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GithubRepo> {
        val page = params.key ?: 1
        val perPage = params.loadSize.coerceAtMost(100)
        return try {
            val response = api.searchRepositories(
                query = query,
                page = page,
                perPage = perPage,
            )
            val nextKey = when {
                response.items.isEmpty() -> null
                page * perPage >= 1000 -> null
                else -> page + 1
            }
            LoadResult.Page(
                data = response.items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
