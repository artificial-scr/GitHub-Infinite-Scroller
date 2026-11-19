package com.github.infinitescroller.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.github.infinitescroller.data.repository.SortOption
import com.github.infinitescroller.testing.repo
import com.github.infinitescroller.testing.testMoshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistenceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(name: String) =
        PreferenceDataStoreFactory.create(scope = scope) { tmp.root.resolve("$name.preferences_pb") }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `v1 tags are migrated and the user skips onboarding`() = runBlocking {
        val ds = store("settings")
        ds.edit { it[stringSetPreferencesKey("selected_tags")] = setOf("kotlin", "android") }
        val repo = DataStoreSettingsRepository(ds)

        val s = repo.settings.first()
        assertEquals(listOf("android", "kotlin"), s.topics)
        assertTrue(s.onboardingDone)

        repo.toggleTopic("rust")
        assertEquals(listOf("android", "kotlin", "rust"), repo.settings.first().topics)
    }

    @Test
    fun `settings round trip and keep topic order`() = runBlocking {
        val repo = DataStoreSettingsRepository(store("settings"))
        assertFalse(repo.settings.first().onboardingDone)

        repo.setTopics(listOf("zig", "go", "zig"))
        repo.setSort(SortOption.RISING)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setToken("  ghp_abc  ")

        val s = repo.settings.first()
        assertEquals(listOf("zig", "go"), s.topics)
        assertEquals(SortOption.RISING, s.sort)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals("ghp_abc", s.token)

        repo.setToken("")
        assertEquals("", repo.settings.first().token)
    }

    @Test
    fun `bookmarks toggle, newest first, and survive reload`() = runBlocking {
        val ds = store("bookmarks")
        val bookmarks = DataStoreBookmarkRepository(ds, testMoshi)

        bookmarks.toggle(repo(1))
        bookmarks.toggle(repo(2))
        assertEquals(listOf(2L, 1L), bookmarks.bookmarks.first().map { it.id })

        bookmarks.toggle(repo(1))
        val reloaded = DataStoreBookmarkRepository(ds, testMoshi).bookmarks.first()
        assertEquals(listOf(repo(2)), reloaded)
    }

    @Test
    fun `topic normalisation`() {
        assertEquals("machine-learning", Topics.normalize("  Machine Learning "))
        assertEquals("home-assistant", Topics.normalize("#home_assistant"))
        assertEquals("c-plus", Topics.normalize("--c--plus--"))
        assertNull(Topics.normalize(""))
        assertNull(Topics.normalize("c++"))
        assertNull(Topics.normalize("a".repeat(51)))
    }
}
