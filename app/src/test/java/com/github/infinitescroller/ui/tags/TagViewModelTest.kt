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

    private class FakeTagStore : TagStore {
        private val _tags = MutableStateFlow<Set<String>>(emptySet())
        override val selectedTags: Flow<Set<String>> = _tags
        override suspend fun saveTags(tags: Set<String>) { _tags.value = tags }
    }

    @Test
    fun `toggle adds a new tag`() = runTest {
        val viewModel = TagViewModel(FakeTagStore())
        viewModel.toggleTag("android")
        assertEquals(setOf("android"), viewModel.selectedTags.value)
    }

    @Test
    fun `toggle removes an existing tag`() = runTest {
        val store = FakeTagStore()
        store.saveTags(setOf("android"))
        val viewModel = TagViewModel(store)
        viewModel.toggleTag("android")
        assertEquals(emptySet<String>(), viewModel.selectedTags.value)
    }

    @Test
    fun `toggle preserves other tags`() = runTest {
        val store = FakeTagStore()
        store.saveTags(setOf("android", "kotlin"))
        val viewModel = TagViewModel(store)
        viewModel.toggleTag("android")
        assertEquals(setOf("kotlin"), viewModel.selectedTags.value)
    }
}
