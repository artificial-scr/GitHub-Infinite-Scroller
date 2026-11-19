package com.github.infinitescroller.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.github.infinitescroller.data.repository.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val topics: List<String> = emptyList(),
    val sort: SortOption = SortOption.STARS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val token: String = "",
    val onboardingDone: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setTopics(topics: List<String>)
    suspend fun toggleTopic(topic: String)
    suspend fun setSort(sort: SortOption)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setToken(token: String)
    suspend fun setOnboardingDone()
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val topics = stringPreferencesKey("topics")
        val sort = stringPreferencesKey("sort")
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val token = stringPreferencesKey("github_token")
        val onboardingDone = booleanPreferencesKey("onboarding_done")

        /** Unordered tag set written by v1.0; read once as a fallback so users keep their picks. */
        val legacyTags = stringSetPreferencesKey("selected_tags")
    }

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val topics = prefs.topics()
            UserSettings(
                topics = topics,
                sort = prefs[Keys.sort].toEnum(SortOption.STARS),
                themeMode = prefs[Keys.theme].toEnum(ThemeMode.SYSTEM),
                dynamicColor = prefs[Keys.dynamicColor] ?: false,
                token = prefs[Keys.token].orEmpty(),
                onboardingDone = prefs[Keys.onboardingDone] ?: topics.isNotEmpty(),
            )
        }
        .distinctUntilChanged()

    override suspend fun setTopics(topics: List<String>) {
        dataStore.edit { it.writeTopics(topics) }
    }

    override suspend fun toggleTopic(topic: String) {
        dataStore.edit { prefs ->
            val current = prefs.topics()
            prefs.writeTopics(if (topic in current) current - topic else current + topic)
        }
    }

    override suspend fun setSort(sort: SortOption) {
        dataStore.edit { it[Keys.sort] = sort.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    override suspend fun setToken(token: String) {
        dataStore.edit {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) it.remove(Keys.token) else it[Keys.token] = trimmed
        }
    }

    override suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.onboardingDone] = true }
    }

    private fun Preferences.topics(): List<String> =
        this[Keys.topics]?.split(',')?.filter { it.isNotBlank() }
            ?: this[Keys.legacyTags]?.sorted()
            ?: emptyList()

    private fun MutablePreferences.writeTopics(topics: List<String>) {
        this[Keys.topics] = topics.distinct().joinToString(",")
        remove(Keys.legacyTags)
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}
