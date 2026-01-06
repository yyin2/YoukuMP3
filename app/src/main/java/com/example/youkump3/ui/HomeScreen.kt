package com.example.youkump3.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.youkump3.logic.ConversionManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(initialUrl: String?, onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repo = remember { HistoryRepository(db.historyDao()) }
    val conversionManager = remember { ConversionManager(context) }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf("") }
    var isConverting by remember { mutableStateOf(false) }
    var resultPath by remember { mutableStateOf<String?>(null) } // To store path for actions

    // Launcher for Save As
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri: Uri? ->
        uri?.let {
            resultPath?.let { path ->
                try {
                    val sourceFile = File(path)
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

    LaunchedEffect(initialUrl) {
        if (initialUrl != null && initialUrl.isNotBlank()) {
            url = initialUrl
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            Button(onClick = onNavigateToHistory, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.history_btn))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.enter_url_hint)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isNotBlank()) {
                    isConverting = true
                    resultPath = null // Reset previous result
                    val currentUrl = url
                    logs = "Starting conversion for: $currentUrl\n"
                    
                    scope.launch {
                        val startTime = System.currentTimeMillis()
                        conversionManager.startConversion(
                            youkuUrl = currentUrl,
                            onLog = { message ->
                                logs += "$message\n"
                            },
                            onFinished = { success, path ->
                                isConverting = false
                                logs += if (success) "FINISHED: Saved to $path\n" else "FAILED.\n"
                                
                                if (success && path != null) {
                                    resultPath = path
                                }
                                
                                val record = ConversionRecord(
                                    originalUrl = currentUrl,
                                    filePath = path,
                                    status = if (success) "SUCCESS" else "FAILED",
                                    timestamp = startTime,
                                    logs = logs
                                )
                                scope.launch {
                                    repo.addRecord(record)
                                }
                            }
                        )
                    }
                }
            },
            enabled = !isConverting && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isConverting) stringResource(R.string.status_processing) else stringResource(R.string.start_conversion))
        }

        // Action Buttons (Show only if successful)
        if (!isConverting && resultPath != null) {
             Spacer(modifier = Modifier.height(16.dp))
             Row {
                Button(
                    onClick = {
                        try {
                            val mp = MediaPlayer()
                            mp.setDataSource(resultPath)
                            mp.prepare()
                            mp.start()
                        } catch (e: Exception) {
                             Toast.makeText(context, context.getString(R.string.preview_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_preview))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                         val fileName = File(resultPath!!).name
                         saveLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_save_as))
                }
             }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.logs_label))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(logs)
        }
    }
}
