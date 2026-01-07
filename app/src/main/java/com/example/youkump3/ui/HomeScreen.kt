package com.example.youkump3.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.youkump3.R
import com.example.youkump3.YoukuApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(initialUrl: String? = null, onNavigateToHistory: () -> Unit, onNavigateToDetail: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as YoukuApp
    val taskManager = app.taskManager

    var url by remember { mutableStateOf("") }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            url = initialUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.enter_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { url = "" },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_clear))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            taskManager.startTask(url)
                            onNavigateToDetail(url)
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_conversion))
                }
            }
        }
    }
}
