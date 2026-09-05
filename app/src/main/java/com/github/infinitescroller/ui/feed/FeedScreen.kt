package com.github.infinitescroller.ui.feed

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.github.infinitescroller.data.model.GithubRepo
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onOpenTags: () -> Unit,
) {
    val repos = viewModel.repos.collectAsLazyPagingItems()
    val context = LocalContext.current
    val isRefreshing = repos.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Repos") },
                actions = {
                    IconButton(onClick = onOpenTags) {
                        Icon(Icons.Default.Tag, contentDescription = "Edit tags")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { repos.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            RepoList(repos = repos, onRepoClick = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: ActivityNotFoundException) {
                    // No browser installed — silently ignore
                }
            })
        }
    }
}

@Composable
private fun RepoList(
    repos: LazyPagingItems<GithubRepo>,
    onRepoClick: (String) -> Unit,
) {
    val refreshState = repos.loadState.refresh

    if (refreshState is LoadState.NotLoading && repos.itemCount == 0) {
        EmptyState()
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (refreshState is LoadState.Error) {
            item {
                ErrorBanner(
                    message = friendlyError(refreshState.error),
                    onRetry = { repos.retry() },
                )
            }
        }

        items(count = repos.itemCount, key = repos.itemKey { it.id }) { index ->
            repos[index]?.let { repo ->
                RepoCard(repo = repo, onClick = { onRepoClick(repo.htmlUrl) })
            }
        }

        when (val append = repos.loadState.append) {
            is LoadState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error -> item {
                ErrorBanner(
                    message = friendlyError(append.error),
                    onRetry = { repos.retry() },
                )
            }
            else -> Unit
        }
    }
}

private fun friendlyError(error: Throwable): String {
    if (error is HttpException && (error.code() == 403 || error.code() == 429)) {
        return "GitHub rate limit exceeded. Please wait a moment and try again."
    }
    return error.message ?: "Something went wrong"
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No repos found.\nTry selecting different tags.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
