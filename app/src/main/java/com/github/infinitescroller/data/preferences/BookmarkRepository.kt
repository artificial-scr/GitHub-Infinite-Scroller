package com.github.infinitescroller.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.infinitescroller.data.model.Repo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

interface BookmarkRepository {
    /** Saved repos, most recently saved first. */
    val bookmarks: Flow<List<Repo>>
    suspend fun toggle(repo: Repo)
}

/** Stores bookmarks as a JSON array; the list is small so a full rewrite per change is fine. */
class DataStoreBookmarkRepository(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi,
) : BookmarkRepository {

    private val key = stringPreferencesKey("bookmarks_json")
    private val adapter: JsonAdapter<List<Repo>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, Repo::class.java))

    override val bookmarks: Flow<List<Repo>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { decode(it[key]) }
        .distinctUntilChanged()

    override suspend fun toggle(repo: Repo) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            val updated = if (current.any { it.id == repo.id }) {
                current.filterNot { it.id == repo.id }
            } else {
                listOf(repo) + current
            }
            prefs[key] = adapter.toJson(updated)
        }
    }

    private fun decode(json: String?): List<Repo> =
        json?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: emptyList()
}
