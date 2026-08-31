package com.github.infinitescroller.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.infinitescroller.data.preferences.TagPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(
    private val tagPreferences: TagPreferences,
) : ViewModel() {

    val selectedTags: StateFlow<Set<String>> = tagPreferences.selectedTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleTag(tag: String) {
        viewModelScope.launch {
            val current = selectedTags.value.toMutableSet()
            if (current.contains(tag)) current.remove(tag) else current.add(tag)
            tagPreferences.saveTags(current)
        }
    }
}
