package com.qalculator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        appState = AppState(this)
        setContent {
            QalculatorApp(appState)
        }
    }

    override fun onPause() {
        super.onPause()
        appState.flushIfPending()
    }

    override fun onStop() {
        super.onStop()
        appState.flushIfPending()
    }
}
