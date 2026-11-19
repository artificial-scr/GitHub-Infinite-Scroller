package com.github.infinitescroller.ui.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.infinitescroller.data.preferences.Topics

/**
 * @param onboarding true on first launch (shows Continue/Skip); false when editing from the feed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicsScreen(
    viewModel: TopicsViewModel,
    onboarding: Boolean,
    onDone: () -> Unit,
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var custom by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submitCustom() {
        if (custom.isBlank()) return
        error = viewModel.addCustom(custom)
        if (error == null) custom = ""
    }

    val curated = Topics.categories.flatMap { it.topics }.toSet()
    val customSelected = selected.filterNot { it in curated }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(if (onboarding) "What do you build?" else "Your topics") },
                navigationIcon = {
                    if (!onboarding) {
                        IconButton(onClick = onDone) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (onboarding) {
                        TextButton(onClick = {
                            viewModel.finishOnboarding()
                            onDone()
                        }) { Text("Skip") }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (onboarding) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = {
                            viewModel.finishOnboarding()
                            onDone()
                        },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .height(52.dp),
                    ) {
                        Text(
                            when (selected.size) {
                                0 -> "Pick at least one topic"
                                1 -> "Start exploring · 1 topic"
                                else -> "Start exploring · ${selected.size} topics"
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Each topic becomes a tab in your feed. Pick as many as you like; you can change them anytime.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = custom,
                onValueChange = {
                    custom = it
                    error = null
                },
                label = { Text("Add any GitHub topic") },
                placeholder = { Text("e.g. home-assistant") },
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = ::submitCustom, enabled = custom.isNotBlank()) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add topic")
                    }
                },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submitCustom() }),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            if (customSelected.isNotEmpty()) {
                SectionTitle("Your own")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    customSelected.forEach { topic ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggle(topic) },
                            label = { Text(topic) },
                            trailingIcon = {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove $topic", modifier = Modifier.size(InputChipDefaults.IconSize))
                            },
                        )
                    }
                }
            }

            Topics.categories.forEach { category ->
                SectionTitle(category.title)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    category.topics.forEach { topic ->
                        val isSelected = topic in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggle(topic) },
                            label = { Text(topic) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(Modifier.padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
