package com.ruhaan.moctale.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.ruhaan.moctale.core.webview.bridges.DownloadBridge
import com.ruhaan.moctale.core.webview.bridges.ShareBridge

@SuppressLint("SetJavaScriptEnabled")
fun WebView.configureMoctaleSettings(context: Context, onDownloadSuccess: () -> Unit) {
    setBackgroundColor(Color.BLACK)

    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.allowFileAccess = true
    settings.allowContentAccess = true

    settings.setSupportZoom(false)

    settings.javaScriptCanOpenWindowsAutomatically = true

    settings.setSupportMultipleWindows(true)

    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

    settings.mediaPlaybackRequiresUserGesture = false

    settings.offscreenPreRaster = true

    settings.userAgentString =
        settings.userAgentString.replace(
            "; wv",
            "",
        ) + " MoctaleAndroidApp"

    CookieManager.getInstance().setAcceptCookie(true)

    CookieManager.getInstance()
        .setAcceptThirdPartyCookies(
            this,
            true,
        )

    overScrollMode = WebView.OVER_SCROLL_NEVER

    addJavascriptInterface(
        ShareBridge(context),
        "AndroidShare",
    )

    addJavascriptInterface(
        DownloadBridge(context, onDownloadSuccess),
        "AndroidDownloader",
    )
}
