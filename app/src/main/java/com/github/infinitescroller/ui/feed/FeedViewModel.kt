package com.github.infinitescroller.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.infinitescroller.data.model.GithubRepo
import com.github.infinitescroller.data.preferences.TagStore
import com.github.infinitescroller.data.repository.GithubRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val repository: GithubRepository,
    private val tagPreferences: TagStore,
) : ViewModel() {

    val repos: Flow<PagingData<GithubRepo>> = tagPreferences.selectedTags
        .flatMapLatest { tags -> repository.getRepos(tags) }
        .cachedIn(viewModelScope)
}
