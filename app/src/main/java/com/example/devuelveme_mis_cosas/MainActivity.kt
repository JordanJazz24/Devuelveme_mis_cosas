package com.example.devuelveme_mis_cosas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.devuelveme_mis_cosas.presentation.navigation.MainScreen
import com.example.devuelveme_mis_cosas.ui.theme.Devuelveme_mis_cosasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent
        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            val onToggleDarkMode = {
                val newValue = !isDarkMode
                isDarkMode = newValue
                prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
            }

            Devuelveme_mis_cosasTheme(darkTheme = isDarkMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val currentIntent by intentState
                MainScreen(
                    intent = currentIntent,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }
}
