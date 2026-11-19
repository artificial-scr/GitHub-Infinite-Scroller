package com.github.infinitescroller

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.infinitescroller.data.preferences.ThemeMode
import com.github.infinitescroller.ui.navigation.AppNavHost
import com.github.infinitescroller.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as InfiniteScrollerApp).container

        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            // Settings load from disk in a few milliseconds; until then the window background shows.
            val current = settings ?: return@setContent

            val darkTheme = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status/navigation bar icons legible when the in-app theme differs from the system's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            AppTheme(darkTheme = darkTheme, dynamicColor = current.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val startWithOnboarding = remember { !current.onboardingDone }
                    AppNavHost(container = container, startWithOnboarding = startWithOnboarding)
                }
            }
        }
    }
}
