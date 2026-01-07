package com.example.youkump3.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.youkump3.YoukuApp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as YoukuApp
    val taskManager = app.taskManager
    
    val taskStateFlow = taskManager.getTaskState(taskId)
    
    if (taskStateFlow == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.not_found))
            Button(onClick = onBack) { Text(stringResource(R.string.back)) }
        }
        return
    }

    val taskState by taskStateFlow.collectAsState()
    
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            taskState.outputFile?.let { path ->
                saveFile(context, path, destUri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        SelectionContainer {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (!taskState.title.isNullOrBlank()) {
                    Text("${stringResource(R.string.label_title)} ${taskState.title}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text("${stringResource(R.string.label_url)} ${taskState.url}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                val statusColor = when (taskState.status) {
                    "SUCCESS" -> Color.Green
                    "FAILED" -> Color.Red
                    else -> Color.Blue
                }
                val statusText = when (taskState.status) {
                    "SUCCESS" -> stringResource(R.string.status_success)
                    "FAILED" -> stringResource(R.string.status_failed)
                    else -> stringResource(R.string.status_running)
                }

                Text("${stringResource(R.string.label_status)} $statusText", style = MaterialTheme.typography.labelLarge, color = statusColor)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.logs_label))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(taskState.logs.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (taskState.status == "SUCCESS" && taskState.outputFile != null) {
                    Row {
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.prepare()
                                    isPlaying = false
                                } else {
                                    if (mediaPlayer == null) {
                                        mediaPlayer = MediaPlayer().apply {
                                            try {
                                                setDataSource(taskState.outputFile)
                                                prepare()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "播放出错", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            setOnCompletionListener { isPlaying = false }
                                        }
                                    }
                                    mediaPlayer?.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "停止" else stringResource(R.string.btn_preview))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = {
                                 val path = taskState.outputFile ?: return@Button
                                 val fileName = File(path).name
                                 saveLauncher.launch(fileName)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
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
