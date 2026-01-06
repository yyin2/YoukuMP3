package com.example.youkump3.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversion_history")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalUrl: String,
    val filePath: String?,
    val status: String, // "SUCCESS", "FAILED"
    val timestamp: Long,
    val logs: String
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ConversionRecord>>

    @Insert
    suspend fun insert(record: ConversionRecord)
}

@Database(entities = [ConversionRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "youku_mp3_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
