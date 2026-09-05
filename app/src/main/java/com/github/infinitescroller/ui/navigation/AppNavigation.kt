package com.github.infinitescroller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.infinitescroller.ui.ViewModelFactory
import com.github.infinitescroller.ui.feed.FeedScreen
import com.github.infinitescroller.ui.feed.FeedViewModel
import com.github.infinitescroller.ui.tags.TagScreen
import com.github.infinitescroller.ui.tags.TagViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val ROUTE_TAGS = "tags"
private const val ROUTE_FEED = "feed"

@Composable
fun AppNavigation(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val tagViewModel: TagViewModel = viewModel(factory = factory)

    // On first launch, jump straight to feed if tags are already stored in DataStore
    LaunchedEffect(Unit) {
        val initialTags = withTimeoutOrNull(3_000L) {
            tagViewModel.selectedTags.drop(1).first { it.isNotEmpty() }
        } ?: tagViewModel.selectedTags.value
        if (initialTags.isNotEmpty()) {
            navController.navigate(ROUTE_FEED) {
                popUpTo(ROUTE_TAGS) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_TAGS) {
        composable(ROUTE_TAGS) {
            TagScreen(
                viewModel = tagViewModel,
                onConfirm = {
                    navController.navigate(ROUTE_FEED) {
                        popUpTo(ROUTE_TAGS) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(ROUTE_FEED) {
            val feedViewModel: FeedViewModel = viewModel(factory = factory)
            FeedScreen(
                viewModel = feedViewModel,
                onOpenTags = {
                    navController.navigate(ROUTE_TAGS) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
