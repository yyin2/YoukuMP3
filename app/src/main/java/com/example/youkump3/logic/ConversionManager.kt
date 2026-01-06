package com.example.youkump3.logic

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversionManager(private val context: Context) {

    private val extractor = YoukuExtractor(context)

    suspend fun startConversion(
        youkuUrl: String, 
        onLog: (String) -> Unit,
        onFinished: (Boolean, String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onLog("Starting extraction process for: $youkuUrl")
                val (mediaUrl, title) = extractor.extractMediaInfo(youkuUrl, onLog)
                
                if (mediaUrl.isBlank()) {
                    onLog("Error: Could not extract media URL.")
                    onFinished(false, null)
                    return@withContext
                }
                
                onLog("Media URL extracted. Starting download and conversion...")
                
                // Output file setup
                // Sanitize title for filename
                val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val fileName = "${safeTitle}.mp3"
                val outputDir = context.externalCacheDir ?: context.cacheDir
                val outputFile = File(outputDir, fileName)
                
                if (!outputDir.exists()) outputDir.mkdirs()

                // FFmpeg command
                // -i <input> -vn (no video) -acodec libmp3lame -q:a 2 <output>
                val cmd = "-i \"$mediaUrl\" -vn -acodec libmp3lame -q:a 2 -y \"${outputFile.absolutePath}\""
                
                onLog("Executing FFmpeg command...")
                
                val session = FFmpegKit.execute(cmd)
                
                if (ReturnCode.isSuccess(session.returnCode)) {
                    onLog("Conversion successful!")
                    onLog("Saved to: ${outputFile.absolutePath}")
                    onFinished(true, outputFile.absolutePath)
                } else {
                    onLog("FFmpeg failed with state: ${session.state} and return code: ${session.returnCode}")
                    onLog("FFmpeg Output: ${session.allLogsAsString}")
                    onFinished(false, null)
                }
                
            } catch (e: Exception) {
                onLog("Exception occurred: ${e.message}")
                e.printStackTrace()
                onFinished(false, null)
            }
        }
    }
}
