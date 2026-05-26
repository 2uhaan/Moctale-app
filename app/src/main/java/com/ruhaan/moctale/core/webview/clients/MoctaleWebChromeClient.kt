package com.ruhaan.moctale.core.webview.clients

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.net.toUri
import com.ruhaan.moctale.core.webview.WebViewScripts
import com.ruhaan.moctale.core.webview.configureMoctaleSettings

class MoctaleWebChromeClient(
    private val context: Context,
    private val mainHandler: Handler,
    private val mainWebView: WebView,
    private val frameLayout: FrameLayout,
    private val onUpdateActiveWebView: (WebView?) -> Unit,
    private val onInternetStateChange: (Boolean) -> Unit,
    private val onCanGoBackChange: (Boolean) -> Unit,
    private val onDownloadSuccess: () -> Unit
) : WebChromeClient() {

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean {

        val result = view?.hitTestResult
        val extraUrl = result?.extra

        if (extraUrl != null) {
            val uri = extraUrl.toUri()
            val host = uri.host.orEmpty()
            val isInternal = host.contains("moctale.in")

            if (!isInternal) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (_: Exception) {}

                return false
            }
        }

        val popupWebView = WebView(context).apply {
            configureMoctaleSettings(context, onDownloadSuccess)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )

            webViewClient = object : WebViewClient() {
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
                        if (view?.parent == null) {
                            frameLayout.addView(view)
                        }
                        return false
                    }

                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)

                        view?.post {
                            (view.parent as? FrameLayout)?.removeView(view)
                            view.destroy()
                        }

                        onUpdateActiveWebView(mainWebView)
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

                    if (request?.isForMainFrame != true) return

                    mainHandler.post { onInternetStateChange(true) }
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?,
                ) {
                    super.onPageFinished(view, url)

                    CookieManager.getInstance().flush()

                    onUpdateActiveWebView(view)
                    onCanGoBackChange(view?.canGoBack() == true)

                    val isErrorPage =
                        url == null || url == "about:blank" || url.startsWith("chrome-error://")

                    if (!isErrorPage) {
                        mainHandler.post { onInternetStateChange(false) }
                    }

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
        }

        val transport = resultMsg?.obj as WebView.WebViewTransport
        transport.webView = popupWebView
        resultMsg.sendToTarget()

        return true
    }

    override fun onCloseWindow(window: WebView?) {
        frameLayout.removeView(window)
        window?.destroy()

        onUpdateActiveWebView(mainWebView)
        onCanGoBackChange(mainWebView.canGoBack())
    }
}
