package com.github.infinitescroller.ui.tags

import com.github.infinitescroller.data.preferences.TagStore
import com.github.infinitescroller.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
    }

    // WhileSubscribed only starts collecting once there is a subscriber.
    // backgroundScope.launch starts a collector so the StateFlow stays live for the test.

    @Test
    fun `toggle adds a new tag`() = runTest {
        val viewModel = TagViewModel(FakeTagStore())
        backgroundScope.launch { viewModel.selectedTags.collect {} }
        viewModel.toggleTag("android")
        assertEquals(setOf("android"), viewModel.selectedTags.value)
    }

    @Test
    fun `toggle removes an existing tag`() = runTest {
        val viewModel = TagViewModel(FakeTagStore(initial = setOf("android")))
        backgroundScope.launch { viewModel.selectedTags.collect {} }
        viewModel.toggleTag("android")
        assertEquals(emptySet<String>(), viewModel.selectedTags.value)
    }

    @Test
    fun `toggle preserves other tags`() = runTest {
        val viewModel = TagViewModel(FakeTagStore(initial = setOf("android", "kotlin")))
        backgroundScope.launch { viewModel.selectedTags.collect {} }
        viewModel.toggleTag("android")
        assertEquals(setOf("kotlin"), viewModel.selectedTags.value)
    }
}
