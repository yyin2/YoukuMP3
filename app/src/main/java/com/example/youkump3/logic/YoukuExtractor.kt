package com.example.youkump3.logic

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class YoukuExtractor(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractMediaUrl(youkuUrl: String, onLog: (String) -> Unit): String = suspendCancellableCoroutine { continuation ->
        onLog("Initializing WebView for extraction...")
        
        // We need to run WebView on the main thread
        Handler(Looper.getMainLooper()).post {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

            var found = false

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url.toString()
                    if (!found && (url.contains(".m3u8") || url.contains(".mp4"))) {
                        // Filter out ads or non-video m3u8 if possible. For now take the first one.
                        // Youku often uses .m3u8 for HLS.
                        onLog("Found potential media URL: $url")
                        // Basic filtering to avoid ads
                        if (!url.contains("ad") && !url.contains("banner")) {
                            found = true
                            webView.stopLoading()
                            // Ensure we only resume once
                            if (continuation.isActive) {
                                continuation.resume(url)
                            }
                            // Cleanup
                            Handler(Looper.getMainLooper()).post {
                                webView.destroy()
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onLog("Page loaded. Waiting for media stream...")
                    // Timeout logic could be added here
                }
            }

            onLog("Loading URL: $youkuUrl")
            webView.loadUrl(youkuUrl)
            
            // Set a timeout to cancel if nothing found
            Handler(Looper.getMainLooper()).postDelayed({
                if (!found && continuation.isActive) {
                    onLog("Timeout: parsing took too long.")
                    continuation.resume("") // Or throw exception
                    webView.destroy()
                }
            }, 30000) // 30 seconds timeout
        }
    }
}
