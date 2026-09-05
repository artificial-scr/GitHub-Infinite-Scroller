package com.github.infinitescroller.ui.tags

import com.github.infinitescroller.data.preferences.TagStore
import com.github.infinitescroller.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeTagStore(initial: Set<String> = emptySet()) : TagStore {
        private val _tags = MutableStateFlow(initial)
        override val selectedTags: Flow<Set<String>> = _tags
        override suspend fun saveTags(tags: Set<String>) { _tags.value = tags }
        override suspend fun toggleTag(tag: String) {
            val current = _tags.value.toMutableSet()
            if (current.contains(tag)) current.remove(tag) else current.add(tag)
            _tags.value = current
        }
        val savedTags: Set<String> get() = _tags.value
    }

    @Test
    fun `toggle adds a new tag`() = runTest {
        val store = FakeTagStore()
        val viewModel = TagViewModel(store)
        viewModel.toggleTag("android")
        assertEquals(setOf("android"), store.savedTags)
    }

    @Test
    fun `toggle removes an existing tag`() = runTest {
        val store = FakeTagStore(initial = setOf("android"))
        val viewModel = TagViewModel(store)
        viewModel.toggleTag("android")
        assertEquals(emptySet<String>(), store.savedTags)
    }

    @Test
    fun `toggle preserves other tags`() = runTest {
        val store = FakeTagStore(initial = setOf("android", "kotlin"))
        val viewModel = TagViewModel(store)
        viewModel.toggleTag("android")
        assertEquals(setOf("kotlin"), store.savedTags)
    }
}
