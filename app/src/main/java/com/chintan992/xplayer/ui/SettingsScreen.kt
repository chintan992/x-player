package com.chintan992.xplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.chintan992.xplayer.ui.theme.BrandAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController?,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    val defaultPlayerType by viewModel.defaultPlayerType.collectAsState()
    var showPlayerDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Player selection bottom sheet (Same as before)
    if (showPlayerDialog) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                 Text(
                    text = "Select Default Player",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                // ExoPlayer option
                val exoSelected = defaultPlayerType == "EXO"
                val exoBgColor by animateColorAsState(
                    targetValue = if (exoSelected) BrandAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = tween(200),
                    label = "exoBg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDefaultPlayerType("EXO")
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showPlayerDialog = false }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = exoSelected,
                        onClick = { 
                            viewModel.updateDefaultPlayerType("EXO")
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showPlayerDialog = false }
                        },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = BrandAccent
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ExoPlayer", fontWeight = FontWeight.Medium)
                        Text(
                            "Recommended • Android native",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // MPV option
                val mpvSelected = defaultPlayerType == "MPV"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateDefaultPlayerType("MPV")
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showPlayerDialog = false }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = mpvSelected,
                        onClick = { 
                            viewModel.updateDefaultPlayerType("MPV")
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showPlayerDialog = false }
                        },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = BrandAccent
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("MPV Player", fontWeight = FontWeight.Medium)
                        Text(
                            "Advanced codec support",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val topBarHeight = 64.dp
    val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = statusBarHeight + topBarHeight
                )
        ) {
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "App Theme",
                    subtitle = "Dark Mode Only",
                    onClick = {}
                )
            }
            
            SettingsSection(title = "Player") {
                SettingsItem(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Default Player",
                    subtitle = if (defaultPlayerType == "MPV") "MPV Player" else "ExoPlayer",
                    onClick = { showPlayerDialog = true }
                )
 
                SettingsItem(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Default Playback Speed",
                    subtitle = "1.0x",
                    onClick = {}
                )
                // Toggle example
                var autoPlay by remember { mutableStateOf(true) }
                SettingsItemToggle(
                    title = "Auto-play Next",
                    checked = autoPlay,
                    onCheckedChange = { autoPlay = it }
                )
            }

            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = com.chintan992.xplayer.BuildConfig.VERSION_NAME,
                    onClick = {}
                )
            }
            
            // Spacer for bottom content padding
            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
        
        // TOP BAR (Overlay)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
        ) {
             TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold, fontSize = 28.sp) },
                navigationIcon = {
                    // Only show back button when we have a nav controller (non-embedded mode)
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsItemToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Spacer(Modifier.width(40.dp)) // Indent to align with text above if no icon
        Text(
            text = title, 
            style = MaterialTheme.typography.bodyLarge,
             modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandAccent,
                checkedTrackColor = BrandAccent.copy(alpha = 0.5f)
            )
        )
    }
}
