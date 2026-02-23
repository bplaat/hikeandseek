/*
 * Copyright (c) 2026 Bastiaan van der Plaat
 *
 * SPDX-License-Identifier: MIT
 */

package nl.bplaat.hikeandseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import nl.bplaat.hikeandseek.ui.theme.HikeAndSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)
        enableEdgeToEdge()
        setContent {
            HikeAndSeekTheme {
                MainContent(activity = this)
            }
        }
    }
}

@Composable
fun MainContent(activity: ComponentActivity) {
    val scannedText = remember { mutableStateOf<String?>(null) }
    val showSettings = remember { mutableStateOf(false) }

    when {
        showSettings.value -> {
            SettingsScreen(
                context = activity,
                onBackToResults = {
                    showSettings.value = false
                }
            )
        }
        scannedText.value == null -> {
            CameraScreen(
                context = activity,
                lifecycleOwner = activity,
                onCapture = { text ->
                    scannedText.value = text
                }
            )
        }
        else -> {
            TextResultsScreen(
                scannedText = scannedText.value!!,
                context = activity,
                onBackToCamera = {
                    scannedText.value = null
                },
                onNavigateToSettings = {
                    showSettings.value = true
                }
            )
        }
    }
}
