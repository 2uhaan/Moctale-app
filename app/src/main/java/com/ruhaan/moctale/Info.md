**If you encounter bugs, here is a quick guide to track them down:**

**MoctaleWebView.kt**: The visual layer. Responsible for rendering the Compose UI, managing state variables (showNoInternet, canGoBack), and the "No Internet" UI layout.

**WebViewConfig.kt**: The configuration layer. Responsible for WebSettings (JavaScript, zoom, multiple windows, cookies, caching). If the web page isn't allowing cookies or scaling correctly, check here.

**clients/MoctaleWebViewClient.kt**: The main navigation handler. Responsible for intercepting URL clicks (e.g., deciding whether to open a link in an external browser or keep it internal), handling page load completion, and triggering error states.

**clients/MoctaleWebChromeClient.kt**: The popup/window handler. Responsible for intercepts involving target="_blank" or window.open(). It spawns a temporary "popup" WebView.

**bridges/ShareBridge.kt**: Responsible for the Android Intent share sheet. Triggered when the web app invokes JavaScript sharing APIs.

**bridges/DownloadBridge.kt**: Responsible for capturing base64 images from the web and writing them to the device's local MediaStore (Gallery).

**WebViewScripts.kt**: Responsible for the raw JavaScript strings injected into the page when it finishes loading (overriding download/share buttons and patching HTML5 videos).