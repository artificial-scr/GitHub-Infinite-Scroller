package com.github.infinitescroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.infinitescroller.ui.ViewModelFactory
import com.github.infinitescroller.ui.navigation.AppNavigation
import com.github.infinitescroller.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = ViewModelFactory(this)
        setContent {
            AppTheme {
                AppNavigation(factory = factory)
            }
        }
    }
}
