package com.example.youkump3.logic

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YoukuExtractor(private val context: Context) {

    // Returns Pair(StreamUrl, Title)
    suspend fun extractMediaInfo(youkuUrl: String, onLog: (String) -> Unit): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            onLog("Initializing yt-dlp extraction...")
            
            // -j: dump-json
            val request = YoutubeDLRequest(youkuUrl)
            request.addOption("-j")
            
            onLog("Executing yt-dlp to fetch metadata...")
            val response = YoutubeDL.getInstance().execute(request)
            
            val jsonString = response.out
            if (jsonString.isNotBlank()) {
                try {
                    val json = JSONObject(jsonString)
                    val url = json.optString("url")
                    val title = json.optString("title", "Unknown_Title")
                    
                    if (url.isNotBlank()) {
                        onLog("Found Stream URL. Title: $title")
                        return@withContext Pair(url, title)
                    } else {
                        onLog("Error: JSON does not contain 'url' field.")
                        return@withContext Pair("", "")
                    }
                } catch (e: Exception) {
                    onLog("Error parsing JSON: ${e.message}")
                    return@withContext Pair("", "")
                }
            } else {
                onLog("Error: yt-dlp returned empty output.")
                return@withContext Pair("", "")
            }
        } catch (e: Exception) {
            onLog("yt-dlp error: ${e.message}")
            e.printStackTrace()
            return@withContext Pair("", "")
        }
    }
}
