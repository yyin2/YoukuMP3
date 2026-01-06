package com.example.youkump3.logic

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YoukuExtractor(private val context: Context) {

    suspend fun extractMediaUrl(youkuUrl: String, onLog: (String) -> Unit): String = withContext(Dispatchers.IO) {
        try {
            onLog("Initializing yt-dlp extraction...")
            
            // Create request to get JSON info without downloading
            // -J: dump JSON
            // --flat-playlist: don't extract playlist items if it is a playlist (speed up)
            val request = YoutubeDLRequest(youkuUrl)
            
            // We want the stream URL. 
            // -g: get-url
            // -f: format selection (best)
            request.addOption("-g")
            
            onLog("Executing yt-dlp for URL: $youkuUrl")
            val response = YoutubeDL.getInstance().execute(request)
            
            val streamUrl = response.out
            if (streamUrl.isNotBlank()) {
                onLog("Stream URL found: $streamUrl")
                return@withContext streamUrl.trim()
            } else {
                onLog("Error: yt-dlp returned empty URL.")
                return@withContext ""
            }
        } catch (e: Exception) {
            onLog("yt-dlp error: ${e.message}")
            e.printStackTrace()
            return@withContext ""
        }
    }
}
