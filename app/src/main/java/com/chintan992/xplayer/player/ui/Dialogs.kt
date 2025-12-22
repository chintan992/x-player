package com.chintan992.xplayer.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chintan992.xplayer.AspectRatioMode
import com.chintan992.xplayer.CustomSelectionDialog
import com.chintan992.xplayer.TrackInfo
import com.chintan992.xplayer.data.SubtitleResult
import com.chintan992.xplayer.ui.theme.BrandAccent

@Composable
fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            LazyColumn {
                items(speeds) { speed ->
                    TextButton(
                        onClick = { onSpeedSelected(speed) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${speed}x",
                            fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                            color = if (speed == currentSpeed) BrandAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TrackSelectorDialog(
    title: String,
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    // Find currently selected track
    val selectedTrack = tracks.find { it.isSelected }

    CustomSelectionDialog(
        title = title,
        items = tracks,
        selectedItem = selectedTrack,
        itemLabel = { "${it.name}${if (it.language != null) " (${it.language})" else ""}" },
        onItemSelected = onTrackSelected,
        onDismiss = onDismiss
    )
}

@Composable
fun SubtitleSelectorDialog(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo?) -> Unit,
    onSearchClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf<TrackInfo?>(null) + tracks
    val selectedTrack = tracks.find { it.isSelected } ?: options.first()

    CustomSelectionDialog(
        title = "Subtitles",
        items = options,
        selectedItem = selectedTrack,
        itemLabel = { track -> 
            track?.let { "${it.name}${if (it.language != null) " (${it.language})" else ""}" } ?: "Off"
        },
        onItemSelected = onTrackSelected,
        onDismiss = onDismiss,
        footer = {
            TextButton(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search Online")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSearchDialog(
    results: List<SubtitleResult>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Download Subtitles",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search movie/show name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { onSearch(query) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    singleLine = true
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandAccent)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(results) { subtitle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDownload(subtitle.downloadUrl) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subtitle.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(subtitle.language, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Icon(Icons.Default.Download, contentDescription = null, tint = BrandAccent)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                        if (results.isEmpty() && query.isNotEmpty()) {
                            item {
                                Text(
                                    "No results found",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AspectRatioSelectorDialog(
    currentMode: AspectRatioMode,
    onModeSelected: (AspectRatioMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aspect Ratio") },
        text = {
            LazyColumn {
                items(AspectRatioMode.entries.toList()) { mode ->
                    TextButton(
                        onClick = { onModeSelected(mode) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mode.displayName,
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentMode) BrandAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
