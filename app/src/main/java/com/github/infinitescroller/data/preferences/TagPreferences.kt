package com.github.infinitescroller.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TagStore {
    val selectedTags: Flow<Set<String>>
    suspend fun saveTags(tags: Set<String>)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tag_prefs")

class TagPreferences(private val context: Context) : TagStore {

    private val selectedTagsKey = stringSetPreferencesKey("selected_tags")

    override val selectedTags: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[selectedTagsKey] ?: emptySet() }

    override suspend fun saveTags(tags: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[selectedTagsKey] = tags
        }
    }
}
