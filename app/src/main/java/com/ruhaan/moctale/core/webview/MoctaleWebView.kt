package com.ruhaan.moctale.core.webview

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    var showNoInternet by remember { mutableStateOf(false) }
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
            update = { frameLayout ->
                frameLayout.visibility =
                    if (showNoInternet) android.view.View.GONE else android.view.View.VISIBLE
            },
            factory = { context ->
                FrameLayout(context).apply {
                    val frameLayout = this
                    
                    lateinit var mainWebView: WebView

                    mainWebView = WebView(context).apply {
                        activeWebView = this
                        
                        configureMoctaleSettings(context) {
                            scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                        }

                        webViewClient = MoctaleWebViewClient(
                            context = context,
                            mainHandler = mainHandler,
                            onUpdateActiveWebView = { activeWebView = it },
                            onInternetStateChange = { showNoInternet = it },
                            onCanGoBackChange = { canGoBack = it }
                        )

                        webChromeClient = MoctaleWebChromeClient(
                            context = context,
                            mainHandler = mainHandler,
                            mainWebView = this,
                            frameLayout = frameLayout,
                            onUpdateActiveWebView = { activeWebView = it },
                            onInternetStateChange = { showNoInternet = it },
                            onCanGoBackChange = { canGoBack = it },
                            onDownloadSuccess = {
                                scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                            }
                        )

                        loadUrl(url)
                    }

                    addView(mainWebView)
                }
            },
        )

        if (showNoInternet) {
            NoInternetScreen(
                onRetry = {
                    showNoInternet = false
                    activeWebView?.reload()
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.statusBarsPadding(),
        )
    }
}

@Composable
fun NoInternetScreen(
    onRetry: () -> Unit,
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "You're offline",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            Text(
                text = "Please check your internet connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
            )

            Spacer(
                modifier = Modifier.height(24.dp),
            )

            Button(
                onClick = onRetry,
            ) {
                Text("Reload")
            }
        }
    }
}
