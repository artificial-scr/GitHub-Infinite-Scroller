package com.github.infinitescroller.ui.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.infinitescroller.data.preferences.SettingsRepository
import com.github.infinitescroller.data.preferences.Topics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val selected: StateFlow<List<String>> = settingsRepository.settings
        .map { it.topics }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggle(topic: String) {
        viewModelScope.launch { settingsRepository.toggleTopic(topic) }
    }

    /**
     * Adds a user-typed topic. Returns an error message to show, or null on success.
     * An already-selected topic is accepted silently.
     */
    fun addCustom(raw: String): String? {
        val topic = Topics.normalize(raw)
            ?: return "Use letters, numbers and hyphens (max 50 characters)"
        viewModelScope.launch {
            val current = settingsRepository.settings.first().topics
            if (topic !in current) settingsRepository.setTopics(current + topic)
        }
        return null
    }

    fun finishOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingDone() }
    }
}
