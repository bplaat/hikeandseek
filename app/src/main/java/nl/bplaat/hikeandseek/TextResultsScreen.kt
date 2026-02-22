/*
 * Copyright (c) 2026 Bastiaan van der Plaat
 *
 * SPDX-License-Identifier: MIT
 */

package nl.bplaat.hikeandseek

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextResultsScreen(
    scannedText: String,
    context: Context,
    onBackToCamera: () -> Unit
) {
    // Handle system back button
    BackHandler(enabled = true) {
        onBackToCamera()
    }

    val locations = LocationParser.parseLocations(scannedText)
    val showDebug = locations.isEmpty()  // Show debug info if no locations parsed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Locations (${locations.size})") },
                navigationIcon = {
                    IconButton(onClick = onBackToCamera) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Camera"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    downloadGPX(context, locations)
                },
                shape = CircleShape,
                containerColor = Color(0xFFFFD700),
                contentColor = Color.Black,
                modifier = Modifier
                    .padding(16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Download GPX",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        if (locations.isEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                item {
                    Text(
                        "No locations found. Raw OCR text:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF0F0)
                        )
                    ) {
                        Text(
                            scannedText.take(1000),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                items(locations) { location ->
                    LocationCard(location)
                }
            }
        }
    }
}

@Composable
private fun LocationCard(location: Location) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = location.id,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = location.description,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "RD: (${String.format("%.0f", location.rdX)}, ${String.format("%.0f", location.rdY)})",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "WGS84: (${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)})",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun downloadGPX(context: Context, locations: List<Location>) {
    try {
        val gpxContent = generateGPX(locations)

        // Create file in cache directory
        val fileName = "hikeandseek_locations_${System.currentTimeMillis()}.gpx"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { output ->
            output.write(gpxContent.toByteArray())
        }

        // Get URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // Create intent to open file
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/gpx+xml")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Try to open with a maps/gpx viewer app
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: show the file in file manager
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(openIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
