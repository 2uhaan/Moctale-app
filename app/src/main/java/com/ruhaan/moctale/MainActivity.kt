package com.ruhaan.moctale

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ruhaan.moctale.features.home.presentation.HomeScreen
import com.ruhaan.moctale.ui.theme.MoctaleTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    installSplashScreen()

    // Hard-lock the phone context to portrait layout before UI creation
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    // Forces dark system bars with white icons, ignoring the device's system theme
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(0xFF080808.toInt()),
        navigationBarStyle = SystemBarStyle.dark(0xFF080808.toInt()),
    )

    setContent { MoctaleTheme { HomeScreen() } }
  }
}
