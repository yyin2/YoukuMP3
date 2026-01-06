package com.example.youkump3.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.youkump3.R
import com.example.youkump3.data.AppDatabase
import com.example.youkump3.data.ConversionRecord
import com.example.youkump3.data.HistoryRepository
import java.io.File
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
            Text(stringResource(R.string.back_to_home))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(R.string.conversion_history), style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(history) { record ->
                HistoryItem(record, onRetry, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(record: ConversionRecord, onRetry: (String) -> Unit, context: Context) {
    val isFailed = record.status != "SUCCESS"
    val isSuccess = record.status == "SUCCESS"
    
    // Save As Launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri: Uri? ->
        uri?.let {
            // Copy file to selected URI
            if (record.filePath != null) {
                try {
                    val sourceFile = File(record.filePath)
                    if (sourceFile.exists()) {
                        context.contentResolver.openOutputStream(it)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.save_failed) + ": File not found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.save_failed) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
            Text("${stringResource(R.string.label_date)} ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))}")
            Text("${stringResource(R.string.label_url)} ${record.originalUrl}", maxLines = 1)
            Row {
                Text(stringResource(R.string.label_status), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (isFailed) stringResource(R.string.status_failed) else stringResource(R.string.status_success), 
                    color = if (isFailed) Color.Red else Color.Green
                )
            }
            if (isFailed) {
                Text(stringResource(R.string.tap_to_retry), style = MaterialTheme.typography.labelSmall)
            }
            if (isSuccess && record.filePath != null) {
                Row {
                    Button(onClick = {
                        try {
                            val mp = MediaPlayer()
                            mp.setDataSource(record.filePath)
                            mp.prepare()
                            mp.start()
                        } catch (e: Exception) {
                             Toast.makeText(context, context.getString(R.string.preview_error), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(stringResource(R.string.btn_preview))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = {
                         // Extract filename from path or default
                         val fileName = File(record.filePath).name
                         saveLauncher.launch(fileName)
                    }) {
                        Text(stringResource(R.string.btn_save_as))
                    }
                }
            }
        }
    }
}
