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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onBackToCamera: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    BackHandler(enabled = true) {
        onBackToCamera()
    }

    val rdxPrefix = PreferencesManager.getRdxPrefix()
    val rdxSuffix = PreferencesManager.getRdxSuffix()
    val rdyPrefix = PreferencesManager.getRdyPrefix()
    val rdySuffix = PreferencesManager.getRdySuffix()

    val locations = remember(rdxPrefix, rdxSuffix, rdyPrefix, rdySuffix, scannedText) {
        mutableStateListOf<Location>().also {
            it.addAll(
                LocationParser.parseLocations(
                    scannedText,
                    rdxPrefix = rdxPrefix,
                    rdxSuffix = rdxSuffix,
                    rdyPrefix = rdyPrefix,
                    rdySuffix = rdySuffix
                )
            )
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    // Dialog state
    var dialogVisible by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogId by remember { mutableStateOf("") }
    var dialogDescription by remember { mutableStateOf("") }
    var dialogRdXRaw by remember { mutableStateOf("") }
    var dialogRdYRaw by remember { mutableStateOf("") }
    var dialogCategory by remember { mutableStateOf(LocationCategory.NORMAL_CHECKPOINT) }

    fun openAddDialog() {
        editingIndex = null
        dialogId = ""
        dialogDescription = ""
        dialogRdXRaw = ""
        dialogRdYRaw = ""
        dialogCategory = LocationCategory.NORMAL_CHECKPOINT
        dialogVisible = true
    }

    fun openEditDialog(index: Int) {
        val loc = locations[index]
        editingIndex = index
        dialogId = loc.id
        dialogDescription = loc.description
        dialogRdXRaw = loc.rdXRaw.toString()
        dialogRdYRaw = loc.rdYRaw.toString()
        dialogCategory = loc.category
        dialogVisible = true
    }

    fun saveDialog() {
        val id = dialogId.trim()
        val description = dialogDescription.trim()
        val rdXScanned = dialogRdXRaw.trim().toIntOrNull() ?: return
        val rdYScanned = dialogRdYRaw.trim().toIntOrNull() ?: return
        if (id.isEmpty() || description.isEmpty()) return

        val rdXConcatenated = (rdxPrefix.toString() + rdXScanned.toString() + rdxSuffix.toString()).toIntOrNull() ?: rdXScanned
        val rdYConcatenated = (rdyPrefix.toString() + rdYScanned.toString() + rdySuffix.toString()).toIntOrNull() ?: rdYScanned

        val location = Location(
            id = id,
            rdX = rdXConcatenated,
            rdY = rdYConcatenated,
            rdXRaw = rdXScanned,
            rdYRaw = rdYScanned,
            description = description,
            category = dialogCategory
        )

        val idx = editingIndex
        if (idx == null) {
            locations.add(location)
        } else {
            locations[idx] = location
        }
        dialogVisible = false
    }

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
                },
                actions = {
                    IconButton(onClick = { openAddDialog() }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Location"
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { downloadGPX(context, locations) },
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
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
                itemsIndexed(locations) { index, location ->
                    LocationCard(
                        location = location,
                        rdxPrefix = rdxPrefix,
                        rdxSuffix = rdxSuffix,
                        rdyPrefix = rdyPrefix,
                        rdySuffix = rdySuffix,
                        onEdit = { openEditDialog(index) },
                        onDelete = { locations.removeAt(index) }
                    )
                }
            }
        }
    }

    if (dialogVisible) {
        LocationEditDialog(
            isEdit = editingIndex != null,
            id = dialogId,
            onIdChange = {
                dialogId = it
                dialogCategory = detectCategory(it.trim())
            },
            description = dialogDescription,
            onDescriptionChange = { dialogDescription = it },
            rdXRaw = dialogRdXRaw,
            onRdXRawChange = { dialogRdXRaw = it },
            rdYRaw = dialogRdYRaw,
            onRdYRawChange = { dialogRdYRaw = it },
            category = dialogCategory,
            onCategoryChange = { dialogCategory = it },
            onConfirm = { saveDialog() },
            onDismiss = { dialogVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationEditDialog(
    isEdit: Boolean,
    id: String,
    onIdChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    rdXRaw: String,
    onRdXRawChange: (String) -> Unit,
    rdYRaw: String,
    onRdYRawChange: (String) -> Unit,
    category: LocationCategory,
    onCategoryChange: (LocationCategory) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Location" else "Add Location") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = id,
                    onValueChange = onIdChange,
                    label = { Text("ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rdXRaw,
                    onValueChange = onRdXRawChange,
                    label = { Text("RD X (raw)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rdYRaw,
                    onValueChange = onRdYRawChange,
                    label = { Text("RD Y (raw)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        LocationCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        cat.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                },
                                onClick = {
                                    onCategoryChange(cat)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LocationCard(
    location: Location,
    rdxPrefix: Int,
    rdxSuffix: Int,
    rdyPrefix: Int,
    rdySuffix: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Color(android.graphics.Color.parseColor(location.category.hexColor)))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
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
                    text = "RD: (${rdxPrefix}${location.rdXRaw}${rdxSuffix}, ${rdyPrefix}${location.rdYRaw}${rdySuffix})",
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
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp, end = 4.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF666666)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

private fun downloadGPX(context: Context, locations: List<Location>) {
    try {
        val gpxContent = generateGPX(locations)

        val fileName = "hikeandseek_locations_${System.currentTimeMillis()}.gpx"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { output ->
            output.write(gpxContent.toByteArray())
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/gpx+xml")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
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

