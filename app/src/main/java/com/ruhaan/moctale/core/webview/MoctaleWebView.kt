package com.ruhaan.moctale.core.webview

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ruhaan.moctale.core.webview.clients.MoctaleWebChromeClient
import com.ruhaan.moctale.core.webview.clients.MoctaleWebViewClient
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MoctaleWebView(url: String) {

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                CookieManager.getInstance().flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    BackHandler(enabled = canGoBack) { activeWebView?.goBack() }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    val frameLayout = this
                    
                    lateinit var mainWebView: WebView
                    
                    val swipeRefreshLayout = SwipeRefreshLayout(context).apply {

                        // Set the background color of the spinning loader (Black)
                        setProgressBackgroundColorSchemeColor(android.graphics.Color.BLACK)

                        // Set the color of the spinning arrow itself (White)
                        setColorSchemeColors(android.graphics.Color.WHITE)

                        setOnRefreshListener {
                            mainWebView.reload()
                        }
                    }

                    mainWebView = WebView(context).apply {
                        activeWebView = this
                        
                        configureMoctaleSettings(context) {
                            scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                        }

                        webViewClient = MoctaleWebViewClient(
                            context = context,
                            mainHandler = mainHandler,
                            onUpdateActiveWebView = { activeWebView = it },
                            onCanGoBackChange = { canGoBack = it },
                            onPageFinishedLoading = {
                                swipeRefreshLayout.isRefreshing = false
                            }
                        )

                        webChromeClient = MoctaleWebChromeClient(
                            context = context,
                            mainHandler = mainHandler,
                            mainWebView = this,
                            frameLayout = frameLayout,
                            onUpdateActiveWebView = { activeWebView = it },
                            onCanGoBackChange = { canGoBack = it },
                            onDownloadSuccess = {
                                scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                            }
                        )

                        loadUrl(url)
                    }

                    swipeRefreshLayout.addView(mainWebView, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                    addView(swipeRefreshLayout, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.statusBarsPadding(),
        )
    }
}

