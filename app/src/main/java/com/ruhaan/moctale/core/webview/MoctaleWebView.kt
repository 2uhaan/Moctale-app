package com.ruhaan.moctale.core.webview

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
  var lastAttemptedUrl by remember { mutableStateOf(url) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val mainHandler = remember { Handler(Looper.getMainLooper()) }

  BackHandler(enabled = canGoBack) { activeWebView?.goBack() }

  Box(
      modifier = Modifier.fillMaxSize(),
  ) {
    var isPageLoading by remember { mutableStateOf(true) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
          FrameLayout(context).apply {
            val frameLayout = this

            lateinit var mainWebView: WebView

            val swipeRefreshLayout =
                SwipeRefreshLayout(context).apply {
                  setProgressBackgroundColorSchemeColor(android.graphics.Color.BLACK)
                  setColorSchemeColors(android.graphics.Color.WHITE)

                  setOnRefreshListener { mainWebView.loadUrl(lastAttemptedUrl) }
                }

            class ScrollBridge(private val onScrollResult: (Boolean) -> Unit) {
              @android.webkit.JavascriptInterface
              fun reportScrollTop(isAtTop: Boolean) {
                mainHandler.post { onScrollResult(isAtTop) }
              }
            }

            mainWebView =
                object : WebView(context) {
                      override fun onProvideAutofillVirtualStructure(
                          structure: android.view.ViewStructure?,
                          flags: Int,
                      ) {
                        // Empty structure to disable autofill system
                      }
                    }
                    .apply {
                      importantForAutofill =
                          android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                      activeWebView = this

                      addJavascriptInterface(
                          ScrollBridge { isAtTop -> swipeRefreshLayout.isEnabled = isAtTop },
                          "ScrollBridge",
                      )

                      configureMoctaleSettings(context) {
                        scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                      }

                      webViewClient =
                          MoctaleWebViewClient(
                              context = context,
                              mainHandler = mainHandler,
                              onUpdateActiveWebView = { activeWebView = it },
                              onCanGoBackChange = { canGoBack = it },
                              onPageFinishedLoading = {
                                swipeRefreshLayout.isRefreshing = false
                                isPageLoading = false
                              },
                              onUrlUpdated = { newUrl -> lastAttemptedUrl = newUrl },
                              onInjectScrollBridge = { view ->
                                view?.evaluateJavascript(
                                    WebViewScripts.injectScrollBridgeScript,
                                    null,
                                )
                              },
                          )

                      webChromeClient =
                          MoctaleWebChromeClient(
                              context = context,
                              mainHandler = mainHandler,
                              mainWebView = this,
                              frameLayout = frameLayout,
                              onUpdateActiveWebView = { activeWebView = it },
                              onCanGoBackChange = { canGoBack = it },
                              onDownloadSuccess = {
                                scope.launch { snackbarHostState.showSnackbar("Saved to gallery") }
                              },
                              onPageStartedLoading = {
                                isPageLoading = true // ADD THIS for popup handling
                              },
                          )

                      loadUrl(url)
                    }

            swipeRefreshLayout.addView(
                mainWebView,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            addView(
                swipeRefreshLayout,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
          }
        },
    )
    // ADD: Loader overlay
    if (isPageLoading) {
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .align(Alignment.Center)
                  .background(Color(0xFF080808)), // ADD THIS
          contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.statusBarsPadding(),
    )
  }
}
