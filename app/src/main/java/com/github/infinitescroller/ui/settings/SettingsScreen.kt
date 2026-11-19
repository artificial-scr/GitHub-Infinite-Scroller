package com.github.infinitescroller.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.infinitescroller.BuildConfig
import com.github.infinitescroller.data.preferences.SettingsRepository
import com.github.infinitescroller.data.preferences.ThemeMode
import com.github.infinitescroller.data.preferences.UserSettings
import com.github.infinitescroller.ui.components.openUrl
import com.github.infinitescroller.ui.theme.supportsDynamicColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SOURCE_URL = "https://github.com/artificial-scr/GitHub-Infinite-Scroller"
private const val NEW_TOKEN_URL = "https://github.com/settings/personal-access-tokens/new"

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    val state: StateFlow<UserSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) = launch { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settings.setDynamicColor(enabled) }
    fun setToken(token: String) = launch { settings.setToken(token) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageTopics: () -> Unit,
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Settings") }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        val current = settings ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Section(Icons.Outlined.Palette, "Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val modes = ThemeMode.entries
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = current.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        }
                    }
                }
                if (supportsDynamicColor) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dynamic color", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Match the colors of your wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = current.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                    }
                }
            }

            Section(Icons.Outlined.Tag, "Feed") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageTopics)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Topics", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = current.topics.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None yet. Showing popular repositories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                }
            }

            Section(Icons.Outlined.Key, "GitHub access") {
                TokenEditor(
                    savedToken = current.token,
                    onSave = viewModel::setToken,
                    onCreateToken = { context.openUrl(NEW_TOKEN_URL) },
                )
            }

            Section(Icons.Outlined.Info, "About") {
                Text("GitHub Infinite Scroller", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { context.openUrl(SOURCE_URL) }) {
                    Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Source code")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TokenEditor(
    savedToken: String,
    onSave: (String) -> Unit,
    onCreateToken: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(savedToken) }
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(savedToken) { draft = savedToken }
    val hasToken = savedToken.isNotBlank()

    Text(
        text = if (hasToken) {
            "Signed requests: up to 30 searches a minute."
        } else {
            "Without a token GitHub allows 10 searches a minute. A token with no extra permissions raises that to 30."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Personal access token") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide token" else "Show token",
                )
            }
        },
        supportingText = { Text("Stored only on this device and sent only to api.github.com.") },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onSave(draft) }, enabled = draft.trim() != savedToken) { Text("Save") }
        Spacer(Modifier.width(8.dp))
        if (hasToken) {
            OutlinedButton(onClick = { onSave("") }) { Text("Remove") }
        } else {
            OutlinedButton(onClick = onCreateToken) { Text("Create a token") }
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
