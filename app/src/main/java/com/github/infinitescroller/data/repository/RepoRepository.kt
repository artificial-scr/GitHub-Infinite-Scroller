package com.github.infinitescroller.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.github.infinitescroller.data.api.GithubApi
import com.github.infinitescroller.data.api.GithubErrorParser
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.data.paging.SearchPagingSource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class RepoRepository(
    private val api: GithubApi,
    private val errorParser: GithubErrorParser,
    private val today: () -> LocalDate = { LocalDate.now() },
) {

    fun feed(request: FeedRequest): Flow<PagingData<Repo>> {
        val query = QueryBuilder.build(request, today())
        return Pager(
            config = PagingConfig(
                // Page numbers map to fixed offsets, so every load must use the same size.
                pageSize = SearchPagingSource.PAGE_SIZE,
                initialLoadSize = SearchPagingSource.PAGE_SIZE,
                prefetchDistance = 8,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                SearchPagingSource(api, errorParser, query, request.sort.apiSort)
            },
        ).flow
    }
}
