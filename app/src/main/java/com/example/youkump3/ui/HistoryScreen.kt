package com.example.youkump3.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.youkump3.data.AppDatabase
import com.example.youkump3.data.ConversionRecord
import com.example.youkump3.data.HistoryRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HistoryScreen(onBack: () -> Unit, onRetry: (String) -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val repo = HistoryRepository(db.historyDao())
    val history by repo.allHistory.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) {
            Text("Back to Home")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Conversion History", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(history) { record ->
                HistoryItem(record, onRetry)
            }
        }
    }
}

@Composable
fun HistoryItem(record: ConversionRecord, onRetry: (String) -> Unit) {
    val isFailed = record.status != "SUCCESS"
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            if (isFailed) {
                onRetry(record.originalUrl)
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))}")
            Text("URL: ${record.originalUrl}", maxLines = 1)
            Row {
                Text("Status: ", color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = record.status, 
                    color = if (isFailed) Color.Red else Color.Green
                )
            }
            if (isFailed) {
                Text("(Tap to Retry)", style = MaterialTheme.typography.labelSmall)
            }
            if (record.filePath != null) {
                Text("Path: ${record.filePath}")
            }
        }
    }
}
