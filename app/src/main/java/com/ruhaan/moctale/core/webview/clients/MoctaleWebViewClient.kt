package com.ruhaan.moctale.core.webview.clients

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.ruhaan.moctale.core.webview.WebViewScripts
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class MoctaleWebViewClient(
    private val context: Context,
    private val mainHandler: Handler,
    private val onUpdateActiveWebView: (WebView?) -> Unit,
    private val onCanGoBackChange: (Boolean) -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val urlString = request?.url?.toString() ?: return null
        val method = request.method

        if (method.equals("GET", ignoreCase = true) &&
            urlString.contains("/_next/static/") &&
            (urlString.endsWith(".js") || urlString.endsWith(".css") ||
             urlString.endsWith(".png") || urlString.endsWith(".webp"))
        ) {
            try {
                val extension = urlString.substringAfterLast('.', "")
                val mimeType = when (extension) {
                    "js" -> "application/javascript"
                    "css" -> "text/css"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "application/octet-stream"
                }

                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(urlString.toByteArray(Charsets.UTF_8))
                val hashString = hashBytes.joinToString("") { "%02x".format(it) }
                val fileName = "$hashString.$extension"

                val cacheDir = File(context.cacheDir, "next_static_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val cacheFile = File(cacheDir, fileName)

                if (cacheFile.exists() && cacheFile.length() > 0) {
                    return WebResourceResponse(mimeType, "UTF-8", FileInputStream(cacheFile))
                }

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val tempFile = File(cacheDir, "$fileName.tmp")
                    connection.inputStream.use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        tempFile.renameTo(cacheFile)
                        return WebResourceResponse(mimeType, "UTF-8", FileInputStream(cacheFile))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {

        onUpdateActiveWebView(view)

        val currentUrl = request?.url?.toString() ?: return false
        val uri = currentUrl.toUri()
        val host = uri.host.orEmpty()

        val isInternal = host.contains("moctale.in")

        if (isInternal) {
            return false
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        android.util.Log.d("WebViewError", "onReceivedError: url=${request?.url}, isMainFrame=${request?.isForMainFrame}, error=${error?.description}")
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        super.onPageFinished(view, url)

        CookieManager.getInstance().flush()

        onUpdateActiveWebView(view)

        onCanGoBackChange(view?.canGoBack() == true)

        view?.evaluateJavascript(WebViewScripts.injectShareScript, null)
        view?.evaluateJavascript(WebViewScripts.injectDownloadScript, null)
        view?.evaluateJavascript(WebViewScripts.injectVideoFixScript, null)
    }

    override fun doUpdateVisitedHistory(
        view: WebView?,
        url: String?,
        isReload: Boolean,
    ) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onCanGoBackChange(view?.canGoBack() == true)
    }
}
