package com.ruhaan.moctale.features.home.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruhaan.moctale.core.webview.MoctaleWebView

@Composable
fun HomeScreen() {

  Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
    MoctaleWebView(url = "https://www.moctale.in/explore")
  }
}
