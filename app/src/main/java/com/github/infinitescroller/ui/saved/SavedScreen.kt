package com.github.infinitescroller.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.data.preferences.BookmarkRepository
import com.github.infinitescroller.ui.components.MessageState
import com.github.infinitescroller.ui.components.RepoCard
import com.github.infinitescroller.ui.components.RepoDetailSheet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(private val bookmarks: BookmarkRepository) : ViewModel() {

    /** null until the first read completes, so the empty state doesn't flash. */
    val saved: StateFlow<List<Repo>?> = bookmarks.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(repo: Repo) {
        viewModelScope.launch { bookmarks.toggle(repo) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(viewModel: SavedViewModel) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var detailRepo by remember { mutableStateOf<Repo?>(null) }
    val savedIds = saved.orEmpty().mapTo(HashSet()) { it.id }

    fun toggleWithUndo(repo: Repo) {
        val wasSaved = repo.id in savedIds
        viewModel.toggle(repo)
        if (wasSaved) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    message = "Removed ${repo.name}",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.toggle(repo)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = saved?.size?.takeIf { it > 0 }?.let { "Saved  ·  $it" } ?: "Saved",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val list = saved
        when {
            list == null -> Unit
            list.isEmpty() -> MessageState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Nothing saved yet",
                body = "Tap the bookmark on any repository to keep it here, even offline.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(list, key = { it.id }) { repo ->
                    RepoCard(
                        repo = repo,
                        isBookmarked = true,
                        onClick = { detailRepo = repo },
                        onToggleBookmark = { toggleWithUndo(repo) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    detailRepo?.let { repo ->
        RepoDetailSheet(
            repo = repo,
            isBookmarked = repo.id in savedIds,
            onToggleBookmark = { toggleWithUndo(repo) },
            onDismiss = { detailRepo = null },
        )
    }
}
