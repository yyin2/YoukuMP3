package com.example.youkump3

import android.app.Application
import com.example.youkump3.data.AppDatabase
import com.example.youkump3.data.HistoryRepository
import com.example.youkump3.logic.TaskManager
import com.yausername.youtubedl_android.YoutubeDL

class YoukuApp : Application() {

    lateinit var taskManager: TaskManager
        private set

    override fun onCreate() {
        super.onCreate()
        YoutubeDL.init(this)
        
        val db = AppDatabase.getDatabase(this)
        val repo = HistoryRepository(db.historyDao())
        taskManager = TaskManager(this, repo)
    }
}
