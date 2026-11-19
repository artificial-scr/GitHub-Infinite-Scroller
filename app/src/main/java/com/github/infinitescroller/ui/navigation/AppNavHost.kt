package com.github.infinitescroller.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.infinitescroller.AppContainer
import com.github.infinitescroller.ui.explore.ExploreScreen
import com.github.infinitescroller.ui.explore.ExploreViewModel
import com.github.infinitescroller.ui.saved.SavedScreen
import com.github.infinitescroller.ui.saved.SavedViewModel
import com.github.infinitescroller.ui.settings.SettingsScreen
import com.github.infinitescroller.ui.settings.SettingsViewModel
import com.github.infinitescroller.ui.topics.TopicsScreen
import com.github.infinitescroller.ui.topics.TopicsViewModel

private object Routes {
    const val ONBOARDING = "onboarding"
    const val TOPICS = "topics"
    const val EXPLORE = "explore"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
}

private enum class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    EXPLORE(Routes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    SAVED(Routes.SAVED, "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun AppNavHost(container: AppContainer, startWithOnboarding: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    val factory = viewModelFactory {
        initializer { ExploreViewModel(container.repoRepository, container.settingsRepository, container.bookmarkRepository) }
        initializer { SavedViewModel(container.bookmarkRepository) }
        initializer { SettingsViewModel(container.settingsRepository) }
        initializer { TopicsViewModel(container.settingsRepository) }
    }

    Scaffold(
        // Each screen draws its own top bar and handles the status bar inset itself.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = expandVertically(tween(250)) + fadeIn(),
                exit = shrinkVertically(tween(250)) + fadeOut(),
            ) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (startWithOnboarding) Routes.ONBOARDING else Routes.EXPLORE,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Routes.ONBOARDING) {
                TopicsScreen(
                    viewModel = viewModel(factory = factory),
                    onboarding = true,
                    onDone = {
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.TOPICS) {
                TopicsScreen(
                    viewModel = viewModel(factory = factory),
                    onboarding = false,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    viewModel = viewModel(factory = factory),
                    onEditTopics = { navController.navigate(Routes.TOPICS) { launchSingleTop = true } },
                )
            }
            composable(Routes.SAVED) {
                SavedScreen(viewModel = viewModel(factory = factory))
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel(factory = factory),
                    onManageTopics = { navController.navigate(Routes.TOPICS) { launchSingleTop = true } },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
