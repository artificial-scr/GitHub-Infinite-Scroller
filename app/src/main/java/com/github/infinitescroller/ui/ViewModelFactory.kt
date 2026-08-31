package com.github.infinitescroller.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.infinitescroller.data.api.RetrofitClient
import com.github.infinitescroller.data.preferences.TagPreferences
import com.github.infinitescroller.data.repository.GithubRepository
import com.github.infinitescroller.ui.feed.FeedViewModel
import com.github.infinitescroller.ui.tags.TagViewModel

class ViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val tagPreferences = TagPreferences(context.applicationContext)
    private val repository = GithubRepository(RetrofitClient.githubApiService)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(FeedViewModel::class.java) ->
            FeedViewModel(repository, tagPreferences) as T
        modelClass.isAssignableFrom(TagViewModel::class.java) ->
            TagViewModel(tagPreferences) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
