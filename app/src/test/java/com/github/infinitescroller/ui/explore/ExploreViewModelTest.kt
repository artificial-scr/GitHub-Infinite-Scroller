package com.github.infinitescroller.ui.explore

import com.github.infinitescroller.data.api.GithubErrorParser
import com.github.infinitescroller.data.preferences.UserSettings
import com.github.infinitescroller.data.repository.RepoRepository
import com.github.infinitescroller.data.repository.SortOption
import com.github.infinitescroller.testing.FakeBookmarkRepository
import com.github.infinitescroller.testing.FakeGithubApi
import com.github.infinitescroller.testing.FakeSettingsRepository
import com.github.infinitescroller.testing.MainDispatcherRule
import com.github.infinitescroller.testing.repo
import com.github.infinitescroller.testing.testMoshi
import com.github.infinitescroller.ui.topics.TopicsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ExploreViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(topics = listOf("android", "kotlin"), onboardingDone = true))
    private val bookmarks = FakeBookmarkRepository()
    private val repoRepository = RepoRepository(FakeGithubApi(), GithubErrorParser(testMoshi)) { LocalDate.of(2026, 11, 19) }

    private fun viewModel() = ExploreViewModel(repoRepository, settings, bookmarks)

    @Test
    fun `defaults to the first topic`() = runTest {
        assertEquals("android", viewModel().uiState.value.activeTopic)
    }

    @Test
    fun `selecting Popular and topics updates the request`() = runTest {
        val vm = viewModel()
        vm.selectTopic("kotlin")
        assertEquals("kotlin", vm.uiState.value.request.topic)
        vm.selectTopic(null)
        assertNull(vm.uiState.value.request.topic)
    }

    @Test
    fun `removing the active topic falls back to Popular`() = runTest {
        val vm = viewModel()
        vm.selectTopic("kotlin")
        settings.toggleTopic("kotlin")
        assertNull(vm.uiState.value.activeTopic)
    }

    @Test
    fun `sort is persisted and search is trimmed`() = runTest {
        val vm = viewModel()
        vm.setSort(SortOption.UPDATED)
        vm.submitSearch("  paging ")
        assertEquals(SortOption.UPDATED, settings.state.value.sort)
        assertEquals("paging", vm.uiState.value.searchText)
        vm.clearSearch()
        assertEquals("", vm.uiState.value.searchText)
    }

    @Test
    fun `bookmark toggling`() = runTest {
        val vm = viewModel()
        vm.toggleBookmark(repo(7))
        assertEquals(listOf(7L), bookmarks.state.value.map { it.id })
        vm.toggleBookmark(repo(7))
        assertEquals(emptyList<Long>(), bookmarks.state.value.map { it.id })
    }

    @Test
    fun `custom topics are normalised and invalid ones rejected`() = runTest {
        val vm = TopicsViewModel(settings)
        assertNull(vm.addCustom("Home Assistant"))
        assertEquals(listOf("android", "kotlin", "home-assistant"), settings.state.value.topics)
        assertNotNull(vm.addCustom("c++"))
        vm.finishOnboarding()
        assertEquals(true, settings.state.value.onboardingDone)
    }
}
