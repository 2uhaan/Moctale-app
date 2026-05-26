package com.ruhaan.moctale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ruhaan.moctale.features.home.presentation.HomeScreen
import com.ruhaan.moctale.ui.theme.MoctaleTheme
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.content.pm.ActivityInfo

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Hard-lock the phone context to portrait layout before UI creation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Forces dark system bars with white icons, ignoring the device's system theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF080808.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF080808.toInt())
        )

        setContent {
            MoctaleTheme {
                HomeScreen()
            }
        }
    }
}