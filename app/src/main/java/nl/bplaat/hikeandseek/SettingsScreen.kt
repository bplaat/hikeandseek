/*
 * Copyright (c) 2026 Bastiaan van der Plaat
 *
 * SPDX-License-Identifier: MIT
 */

package nl.bplaat.hikeandseek

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    onBackToResults: () -> Unit
) {
    BackHandler(enabled = true) {
        onBackToResults()
    }

    val rdxPrefixState = remember { mutableStateOf(PreferencesManager.getRdxPrefix().toString()) }
    val rdxSuffixState = remember { mutableStateOf(PreferencesManager.getRdxSuffix().toString()) }
    val rdyPrefixState = remember { mutableStateOf(PreferencesManager.getRdyPrefix().toString()) }
    val rdySuffixState = remember { mutableStateOf(PreferencesManager.getRdySuffix().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coordinate Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackToResults) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                "RD X Coordinate Offsets",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = rdxPrefixState.value,
                onValueChange = { rdxPrefixState.value = it },
                label = { Text("RDX Prefix Offset") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = rdxSuffixState.value,
                onValueChange = { rdxSuffixState.value = it },
                label = { Text("RDX Suffix Offset") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text(
                "RD Y Coordinate Offsets",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = rdyPrefixState.value,
                onValueChange = { rdyPrefixState.value = it },
                label = { Text("RDY Prefix Offset") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = rdySuffixState.value,
                onValueChange = { rdySuffixState.value = it },
                label = { Text("RDY Suffix Offset") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    val rdxPrefix = rdxPrefixState.value.toIntOrNull() ?: 0
                    val rdxSuffix = rdxSuffixState.value.toIntOrNull() ?: 0
                    val rdyPrefix = rdyPrefixState.value.toIntOrNull() ?: 0
                    val rdySuffix = rdySuffixState.value.toIntOrNull() ?: 0

                    PreferencesManager.setOffsets(rdxPrefix, rdxSuffix, rdyPrefix, rdySuffix)
                    onBackToResults()
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
