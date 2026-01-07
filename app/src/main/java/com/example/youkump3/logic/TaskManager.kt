package com.example.youkump3.logic

import android.content.Context
import com.example.youkump3.data.ConversionRecord
import com.example.youkump3.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class TaskState(
    val taskId: String,
    val url: String,
    val status: String, // "RUNNING", "SUCCESS", "FAILED"
    val logs: List<String>,
    val outputFile: String? = null,
    val startTime: Long = 0L,
    val dbId: Long = 0L
)

class TaskManager(
    private val context: Context,
    private val repository: HistoryRepository
) {
    private val _taskStates = ConcurrentHashMap<String, MutableStateFlow<TaskState>>()
    private val conversionManager = ConversionManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getTaskState(url: String): StateFlow<TaskState>? {
        return _taskStates[url]?.asStateFlow()
    }

    fun startTask(url: String) {
        if (_taskStates.containsKey(url)) {
            val currentState = _taskStates[url]?.value
            if (currentState?.status == "RUNNING") {
                return 
            }
        }

        val initialState = TaskState(
            taskId = url,
            url = url,
            status = "RUNNING",
            logs = listOf("准备转换: $url"),
            startTime = System.currentTimeMillis()
        )
        val stateFlow = MutableStateFlow(initialState)
        _taskStates[url] = stateFlow

        scope.launch {
            // 1. Create initial record in DB immediately
            val initialRecord = ConversionRecord(
                originalUrl = url,
                filePath = null,
                status = "RUNNING",
                timestamp = initialState.startTime,
                logs = initialState.logs.joinToString("\n")
            )
            val insertedId = repository.addRecord(initialRecord)
            
            // 2. Update state with DB ID
            stateFlow.value = stateFlow.value.copy(dbId = insertedId)

            // 3. Start conversion
            conversionManager.startConversion(
                youkuUrl = url,
                onLog = { msg ->
                    val current = stateFlow.value
                    val newLogs = current.logs + msg
                    stateFlow.value = current.copy(logs = newLogs)
                    
                    // Optional: Update DB logs periodically or at the end. 
                    // Let's update at the end for performance, or every few logs.
                },
                onFinished = { success, path ->
                    val current = stateFlow.value
                    val finalStatus = if (success) "SUCCESS" else "FAILED"
                    val finalLogs = current.logs + (if (success) "转换完成: $path" else "转换失败")
                    
                    stateFlow.value = current.copy(
                        status = finalStatus,
                        outputFile = path,
                        logs = finalLogs
                    )

                    // 4. Update the DB record on finish
                    scope.launch {
                        val record = ConversionRecord(
                            id = current.dbId,
                            originalUrl = url,
                            filePath = path,
                            status = finalStatus,
                            timestamp = current.startTime,
                            logs = finalLogs.joinToString("\n")
                        )
                        repository.updateRecord(record)
                    }
                }
            )
        }
    }
    
    fun clearTask(url: String) {
        _taskStates.remove(url)
    }
}
