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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
fun HistoryScreen(onBack: () -> Unit, onNavigateToDetail: (String) -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val repo = HistoryRepository(db.historyDao())
    val history by repo.allHistory.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversion_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(history) { record ->
                HistoryItem(record, onNavigateToDetail, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(record: ConversionRecord, onNavigateToDetail: (String) -> Unit, context: Context) {
    val isFailed = record.status == "FAILED"
    val isSuccess = record.status == "SUCCESS"
    val isRunning = record.status == "RUNNING"
    
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri: Uri? ->
        uri?.let {
            if (record.filePath != null) {
                saveFile(context, record.filePath, it)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            onNavigateToDetail(record.originalUrl)
        }
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!record.title.isNullOrBlank()) {
                    Text("${stringResource(R.string.label_title)} ${record.title}", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text("${stringResource(R.string.label_date)} ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))}")
                Text("${stringResource(R.string.label_url)} ${record.originalUrl}")
                Row {
                    Text(stringResource(R.string.label_status), color = MaterialTheme.colorScheme.onSurface)
                    val statusText = when (record.status) {
                        "SUCCESS" -> stringResource(R.string.status_success)
                        "FAILED" -> stringResource(R.string.status_failed)
                        else -> stringResource(R.string.status_running)
                    }
                    val statusColor = when (record.status) {
                        "SUCCESS" -> Color.Green
                        "FAILED" -> Color.Red
                        else -> Color.Blue
                    }
                    Text(text = statusText, color = statusColor)
                }
                if (isFailed) {
                    Text(stringResource(R.string.tap_to_retry), style = MaterialTheme.typography.labelSmall)
                }
                if (isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                }
                
                // Buttons if success
                if (isSuccess && record.filePath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                             if (isPlaying) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                             } else {
                                mediaPlayer = MediaPlayer().apply {
                                    try {
                                        setDataSource(record.filePath)
                                        prepare()
                                        start()
                                        setOnCompletionListener { 
                                            isPlaying = false 
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                        }
                                    } catch (e: Exception) {
                                         Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                                         return@apply
                                    }
                                }
                                isPlaying = true
                             }
                        }) {
                            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlaying) "Stop" else stringResource(R.string.btn_preview))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(onClick = {
                             val fileName = File(record.filePath).name
                             saveLauncher.launch(fileName)
                        }) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_save_as))
                        }
                    }
                }
            }
        }
    }
}

private fun saveFile(context: Context, sourcePath: String, destUri: Uri) {
    try {
        val sourceFile = File(sourcePath)
        if (sourceFile.exists()) {
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
         Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
    }
}
