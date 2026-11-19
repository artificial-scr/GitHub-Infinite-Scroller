package com.github.infinitescroller.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.data.preferences.BookmarkRepository
import com.github.infinitescroller.data.preferences.SettingsRepository
import com.github.infinitescroller.data.repository.FeedRequest
import com.github.infinitescroller.data.repository.RepoRepository
import com.github.infinitescroller.data.repository.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val topics: List<String> = emptyList(),
    /** null means the "Popular" feed. */
    val activeTopic: String? = null,
    val sort: SortOption = SortOption.STARS,
    val searchText: String = "",
    val hasToken: Boolean = false,
    val loaded: Boolean = false,
) {
    val request: FeedRequest get() = FeedRequest(activeTopic, sort, searchText)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
    repoRepository: RepoRepository,
    private val settingsRepository: SettingsRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    /** The user's chosen chip; `UNSET` until they pick one, so we can default to their first topic. */
    private val selectedTopic = MutableStateFlow<Selection>(Selection.Unset)
    private val searchText = MutableStateFlow("")

    val uiState: StateFlow<ExploreUiState> = combine(
        settingsRepository.settings,
        selectedTopic,
        searchText,
    ) { settings, selection, text ->
        val active = when (selection) {
            Selection.Unset -> settings.topics.firstOrNull()
            is Selection.Topic -> selection.topic?.takeIf { it in settings.topics }
        }
        ExploreUiState(
            topics = settings.topics,
            activeTopic = active,
            sort = settings.sort,
            searchText = text,
            hasToken = settings.token.isNotBlank(),
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreUiState())

    val repos: Flow<PagingData<Repo>> = uiState
        .map { if (it.loaded) it.request else null }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { repoRepository.feed(it) }
        .cachedIn(viewModelScope)

    val bookmarkedIds: StateFlow<Set<Long>> = bookmarkRepository.bookmarks
        .map { list -> list.mapTo(HashSet()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun selectTopic(topic: String?) {
        selectedTopic.value = Selection.Topic(topic)
    }

    fun setSort(sort: SortOption) {
        viewModelScope.launch { settingsRepository.setSort(sort) }
    }

    fun submitSearch(text: String) {
        searchText.update { text.trim() }
    }

    fun clearSearch() {
        searchText.value = ""
    }

    fun toggleBookmark(repo: Repo) {
        viewModelScope.launch { bookmarkRepository.toggle(repo) }
    }

    private sealed interface Selection {
        data object Unset : Selection
        data class Topic(val topic: String?) : Selection
    }
}
