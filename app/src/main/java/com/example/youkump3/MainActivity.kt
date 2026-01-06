package com.example.youkump3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.youkump3.ui.HomeScreen
import com.example.youkump3.ui.HistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YoukuApp(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intent if app is already running
        setIntent(intent)
    }
}

@Composable
fun YoukuApp(intent: Intent?) {
    var sharedUrl by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf("home") }

    LaunchedEffect(intent) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            initialUrl = sharedUrl,
            onNavigateToHistory = { currentScreen = "history" }
        )
        "history" -> HistoryScreen(
            onBack = { currentScreen = "home" },
            onRetry = { url ->
                sharedUrl = url
                currentScreen = "home"
            }
        )
    }
}
