package com.github.infinitescroller

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.infinitescroller.data.api.GithubClient
import com.github.infinitescroller.data.api.GithubErrorParser
import com.github.infinitescroller.data.preferences.BookmarkRepository
import com.github.infinitescroller.data.preferences.DataStoreBookmarkRepository
import com.github.infinitescroller.data.preferences.DataStoreSettingsRepository
import com.github.infinitescroller.data.preferences.SettingsRepository
import com.github.infinitescroller.data.repository.RepoRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// "tag_prefs" is the file name used by v1.0, kept so existing topic picks carry over.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tag_prefs")
private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

/** Manual dependency container; one instance per process, owned by [InfiniteScrollerApp]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext.settingsDataStore)

    val bookmarkRepository: BookmarkRepository = DataStoreBookmarkRepository(appContext.bookmarkDataStore, moshi)

    val repoRepository: RepoRepository by lazy {
        val api = GithubClient.create(moshi, appContext.cacheDir) {
            // Runs on an OkHttp worker thread; DataStore serves this from memory after the first read.
            runBlocking { settingsRepository.settings.first().token }
        }
        RepoRepository(api, GithubErrorParser(moshi))
    }
}

class InfiniteScrollerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
