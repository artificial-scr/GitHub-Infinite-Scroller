package com.github.infinitescroller.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.infinitescroller.data.preferences.TagStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(
    private val tagStore: TagStore,
) : ViewModel() {

    val selectedTags: StateFlow<Set<String>> = tagStore.selectedTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleTag(tag: String) {
        viewModelScope.launch {
            val current = tagStore.selectedTags.first().toMutableSet()
            if (current.contains(tag)) current.remove(tag) else current.add(tag)
            tagStore.saveTags(current)
        }
    }
}
