package com.github.infinitescroller.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.github.infinitescroller.data.api.GithubException
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.data.paging.SearchPagingSource
import com.github.infinitescroller.data.repository.SortOption
import com.github.infinitescroller.ui.components.EndOfListFooter
import com.github.infinitescroller.ui.components.InlineError
import com.github.infinitescroller.ui.components.LoadingFooter
import com.github.infinitescroller.ui.components.MessageState
import com.github.infinitescroller.ui.components.RepoCard
import com.github.infinitescroller.ui.components.RepoCardSkeleton
import com.github.infinitescroller.ui.components.RepoDetailSheet
import com.github.infinitescroller.ui.components.RetryButton
import com.github.infinitescroller.ui.util.Format
import com.github.infinitescroller.ui.util.toErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onEditTopics: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val repos = viewModel.repos.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var searching by rememberSaveable { mutableStateOf(state.searchText.isNotEmpty()) }
    var detailRepo by remember { mutableStateOf<Repo?>(null) }

    // A new feed always starts at the top.
    LaunchedEffect(state.request) { listState.scrollToItem(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                if (searching) {
                    SearchBarRow(
                        initial = state.searchText,
                        onSubmit = viewModel::submitSearch,
                        onClose = {
                            searching = false
                            viewModel.clearSearch()
                        },
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    ExploreTopBar(
                        state = state,
                        onSearch = { searching = true },
                        onSort = viewModel::setSort,
                        scrollBehavior = scrollBehavior,
                    )
                }
                TopicChips(
                    topics = state.topics,
                    active = state.activeTopic,
                    onSelect = viewModel::selectTopic,
                    onEdit = onEditTopics,
                )
            }
        },
        floatingActionButton = {
            val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
            AnimatedVisibility(visible = showScrollTop, enter = scaleIn(), exit = scaleOut()) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Back to top")
                }
            }
        },
    ) { padding ->
        Feed(
            repos = repos,
            listState = listState,
            bookmarkedIds = bookmarkedIds,
            hasToken = state.hasToken,
            emptyBody = when {
                state.searchText.isNotEmpty() -> "Nothing matches “${state.searchText}”. Try different words or another topic."
                state.activeTopic != null -> "No repositories are tagged #${state.activeTopic} yet. Try a different sort or topic."
                else -> "Try a different sort order."
            },
            onOpen = { detailRepo = it },
            onToggleBookmark = viewModel::toggleBookmark,
            modifier = Modifier.padding(padding),
        )
    }

    detailRepo?.let { repo ->
        RepoDetailSheet(
            repo = repo,
            isBookmarked = repo.id in bookmarkedIds,
            onToggleBookmark = { viewModel.toggleBookmark(repo) },
            onDismiss = { detailRepo = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreTopBar(
    state: ExploreUiState,
    onSearch: () -> Unit,
    onSort: (SortOption) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text("Explore", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = listOfNotNull(state.sort.label, state.activeTopic?.let { "#$it" }).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            menuOpen = false
                            onSort(option)
                        },
                        trailingIcon = {
                            if (option == state.sort) Icon(Icons.Outlined.Check, contentDescription = "Selected")
                        },
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarRow(
    initial: String,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { if (initial.isEmpty()) focusRequester.requestFocus() }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
            }
        },
        title = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Search repositories") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSubmit(text)
                    keyboard?.hide()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        actions = {
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onSubmit("")
                    focusRequester.requestFocus()
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicChips(
    topics: List<String>,
    active: String?,
    onSelect: (String?) -> Unit,
    onEdit: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        item(key = "popular") {
            FilterChip(
                selected = active == null,
                onClick = { onSelect(null) },
                label = { Text("Popular") },
                leadingIcon = {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
        items(topics, key = { it }) { topic ->
            FilterChip(
                selected = topic == active,
                onClick = { onSelect(topic) },
                label = { Text(topic) },
            )
        }
        item(key = "edit") {
            AssistChip(
                onClick = onEdit,
                label = { Text(if (topics.isEmpty()) "Add topics" else "Edit") },
                leadingIcon = {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Feed(
    repos: LazyPagingItems<Repo>,
    listState: LazyListState,
    bookmarkedIds: Set<Long>,
    hasToken: Boolean,
    emptyBody: String,
    onOpen: (Repo) -> Unit,
    onToggleBookmark: (Repo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh = repos.loadState.refresh
    val append = repos.loadState.append

    // Only show the pull indicator for a user-initiated refresh; the first load uses skeletons.
    var userRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) { if (refresh !is LoadState.Loading) userRefreshing = false }

    // When GitHub tells us when the rate limit resets, retry by ourselves at that moment.
    val rateLimit = (refresh as? LoadState.Error)?.error as? GithubException.RateLimited
        ?: (append as? LoadState.Error)?.error as? GithubException.RateLimited
    LaunchedEffect(rateLimit) {
        val retryAt = rateLimit?.retryAtEpochSeconds ?: return@LaunchedEffect
        // Never sooner than 5s, so a reset time already in the past can't cause a tight retry loop.
        delay((Format.secondsUntil(retryAt) + 1).coerceAtLeast(5) * 1_000)
        repos.retry()
    }

    PullToRefreshBox(
        isRefreshing = userRefreshing,
        onRefresh = {
            userRefreshing = true
            repos.refresh()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            repos.itemCount == 0 && refresh is LoadState.Error -> {
                val message = refresh.error.toErrorMessage(hasToken)
                MessageState(
                    icon = refresh.error.icon(),
                    title = message.title,
                    body = message.body,
                    action = { RetryButton(onClick = repos::retry) },
                )
            }
            repos.itemCount == 0 && refresh is LoadState.NotLoading && append.endOfPaginationReached -> {
                MessageState(icon = Icons.Outlined.SearchOff, title = "No repositories found", body = emptyBody)
            }
            repos.itemCount == 0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(6) { RepoCardSkeleton() }
                }
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (refresh is LoadState.Error) {
                    item(key = "refresh-error") {
                        val message = refresh.error.toErrorMessage(hasToken)
                        InlineError(message.title, message.body, onRetry = repos::retry)
                    }
                }
                items(count = repos.itemCount, key = repos.itemKey { it.id }) { index ->
                    repos[index]?.let { repo ->
                        RepoCard(
                            repo = repo,
                            isBookmarked = repo.id in bookmarkedIds,
                            onClick = { onOpen(repo) },
                            onToggleBookmark = { onToggleBookmark(repo) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                item(key = "footer") {
                    when {
                        append is LoadState.Loading -> LoadingFooter()
                        append is LoadState.Error -> {
                            val message = append.error.toErrorMessage(hasToken)
                            InlineError(message.title, message.body, onRetry = repos::retry)
                        }
                        append.endOfPaginationReached -> EndOfListFooter(
                            if (repos.itemCount >= SearchPagingSource.MAX_RESULTS * 9 / 10) {
                                "That's the top ${repos.itemCount}. GitHub search stops at 1,000 results, so try a narrower topic or search."
                            } else {
                                "You've reached the end"
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun Throwable.icon(): ImageVector = when (this) {
    is GithubException.RateLimited -> Icons.Outlined.HourglassTop
    is GithubException.Unauthorized -> Icons.Outlined.Key
    is IOException -> Icons.Outlined.CloudOff
    else -> Icons.Outlined.ErrorOutline
}
