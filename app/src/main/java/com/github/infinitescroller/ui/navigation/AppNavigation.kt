package com.github.infinitescroller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.infinitescroller.ui.ViewModelFactory
import com.github.infinitescroller.ui.feed.FeedScreen
import com.github.infinitescroller.ui.feed.FeedViewModel
import com.github.infinitescroller.ui.tags.TagScreen
import com.github.infinitescroller.ui.tags.TagViewModel

private const val ROUTE_TAGS = "tags"
private const val ROUTE_FEED = "feed"

@Composable
fun AppNavigation(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val tagViewModel: TagViewModel = viewModel(factory = factory)
    val selectedTags by tagViewModel.selectedTags.collectAsState()

    // Once DataStore has emitted and tags exist, jump straight to feed on first launch
    LaunchedEffect(selectedTags) {
        val current = navController.currentBackStackEntry?.destination?.route
        if (selectedTags.isNotEmpty() && current == ROUTE_TAGS) {
            navController.navigate(ROUTE_FEED) {
                popUpTo(ROUTE_TAGS) { inclusive = true }
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
                    }
                },
            )
        }
        composable(ROUTE_FEED) {
            val feedViewModel: FeedViewModel = viewModel(factory = factory)
            FeedScreen(
                viewModel = feedViewModel,
                onOpenTags = { navController.navigate(ROUTE_TAGS) },
            )
        }
    }
}
