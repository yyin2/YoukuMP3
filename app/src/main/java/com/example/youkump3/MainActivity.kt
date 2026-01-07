package com.example.youkump3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.youkump3.ui.HistoryScreen
import com.example.youkump3.ui.HomeScreen
import com.example.youkump3.ui.TaskDetailScreen

sealed class Screen {
    object Home : Screen()
    object History : Screen()
    data class Detail(val taskId: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle shared text (URL)
        val initialUrl = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(initialUrl)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(initialUrl: String?) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var startUrl by remember { mutableStateOf(initialUrl) }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                initialUrl = startUrl,
                onNavigateToHistory = { currentScreen = Screen.History },
                onNavigateToDetail = { taskId -> currentScreen = Screen.Detail(taskId) }
            )
        }
        is Screen.History -> {
            HistoryScreen(
                onBack = { currentScreen = Screen.Home },
                onNavigateToDetail = { taskId -> currentScreen = Screen.Detail(taskId) }
            )
        }
        is Screen.Detail -> {
            TaskDetailScreen(
                taskId = screen.taskId,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}
