package com.example.youkump3.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<ConversionRecord>> = historyDao.getAll()

    suspend fun addRecord(record: ConversionRecord): Long {
        return historyDao.insert(record)
    }

    suspend fun updateRecord(record: ConversionRecord) {
        historyDao.update(record)
    }
}
