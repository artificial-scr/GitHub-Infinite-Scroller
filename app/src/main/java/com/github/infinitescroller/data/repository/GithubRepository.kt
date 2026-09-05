package com.github.infinitescroller.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.github.infinitescroller.data.api.GithubApiService
import com.github.infinitescroller.data.model.GithubRepo
import com.github.infinitescroller.data.paging.GithubPagingSource
import kotlinx.coroutines.flow.Flow

class GithubRepository(private val api: GithubApiService) {

    fun getRepos(tags: Set<String>): Flow<PagingData<GithubRepo>> {
        val query = buildQuery(tags)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { GithubPagingSource(api, query) },
        ).flow
    }

    internal fun buildQuery(tags: Set<String>): String {
        return if (tags.isEmpty()) {
            "stars:>1000"
        } else {
            tags.joinToString(separator = " OR ") { "topic:$it" }
        }
    }
}
