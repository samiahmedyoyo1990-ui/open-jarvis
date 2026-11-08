package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.JarvisViewModel
import com.example.ui.navigation.AppNavigation
import com.example.util.configureAppCheck
import com.example.voice.JarvisWakeWordService

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == JarvisWakeWordService.ACTION_WAKE_WORD_DETECTED) {
                viewModel.handleWakeWordTriggered()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Check if launched by wake word trigger
        checkWakeWordIntent(intent)
        checkShareIntent(intent)

        // Register local broadcast receiver for wake-word activation while app is in foreground
        val filter = IntentFilter(JarvisWakeWordService.ACTION_WAKE_WORD_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }

        // STEP 2: Initialize UI
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF070A12)
            ) {
                AppNavigation(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkWakeWordIntent(intent)
        checkShareIntent(intent)
    }

    private fun checkWakeWordIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(JarvisWakeWordService.EXTRA_WAKE_WORD_TRIGGERED, false) == true) {
            viewModel.handleWakeWordTriggered()
        }
    }

    private fun checkShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            viewModel.handleSharedContent(text, uri)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        super.onDestroy()
    }
}
