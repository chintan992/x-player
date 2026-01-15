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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
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
    // Collect all settings
    val defaultPlayerType by viewModel.defaultPlayerType.collectAsState()
    val defaultOrientation by viewModel.defaultOrientation.collectAsState()
    val defaultSpeed by viewModel.defaultSpeed.collectAsState()
    val defaultAspectRatio by viewModel.defaultAspectRatio.collectAsState()
    val defaultDecoder by viewModel.defaultDecoder.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()
    val longPressSpeed by viewModel.longPressSpeed.collectAsState()
    val controlsTimeout by viewModel.controlsTimeout.collectAsState()
    val resumePlayback by viewModel.resumePlayback.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    
    // Dialog state variables
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showSeekDurationDialog by remember { mutableStateOf(false) }
    var showLongPressSpeedDialog by remember { mutableStateOf(false) }
    var showControlsTimeoutDialog by remember { mutableStateOf(false) }
    
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
    
    // Speed selection bottom sheet
    if (showSpeedDialog) {
        SettingsOptionSheet(
            title = "Playback Speed",
            options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f),
            selectedOption = defaultSpeed,
            optionLabel = { "${it}x" },
            onOptionSelected = {
                viewModel.updateDefaultSpeed(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false },
            sheetState = sheetState
        )
    }
    
    // Aspect Ratio selection bottom sheet
    if (showAspectRatioDialog) {
        SettingsOptionSheet(
            title = "Aspect Ratio",
            options = listOf("FIT", "FILL", "ZOOM", "STRETCH", "RATIO_16_9", "RATIO_4_3"),
            selectedOption = defaultAspectRatio,
            optionLabel = { 
                when (it) {
                    "FIT" -> "Fit"
                    "FILL" -> "Fill"
                    "ZOOM" -> "Zoom"
                    "STRETCH" -> "Stretch"
                    "RATIO_16_9" -> "16:9"
                    "RATIO_4_3" -> "4:3"
                    else -> it
                }
            },
            onOptionSelected = {
                viewModel.updateDefaultAspectRatio(it)
                showAspectRatioDialog = false
            },
            onDismiss = { showAspectRatioDialog = false },
            sheetState = sheetState
        )
    }
    
    // Decoder selection bottom sheet
    if (showDecoderDialog) {
        SettingsOptionSheet(
            title = "Decoder Mode",
            options = listOf("AUTO", "SW", "HW"),
            selectedOption = defaultDecoder,
            optionLabel = { 
                when (it) {
                    "AUTO" -> "Auto"
                    "SW" -> "Software"
                    "HW" -> "Hardware"
                    else -> it
                }
            },
            onOptionSelected = {
                viewModel.updateDefaultDecoder(it)
                showDecoderDialog = false
            },
            onDismiss = { showDecoderDialog = false },
            sheetState = sheetState
        )
    }
    
    // Seek Duration selection bottom sheet
    if (showSeekDurationDialog) {
        SettingsOptionSheet(
            title = "Double-tap Seek Duration",
            options = listOf(5, 10, 15, 30),
            selectedOption = seekDuration,
            optionLabel = { "${it} seconds" },
            onOptionSelected = {
                viewModel.updateSeekDuration(it)
                showSeekDurationDialog = false
            },
            onDismiss = { showSeekDurationDialog = false },
            sheetState = sheetState
        )
    }
    
    // Long-press Speed selection bottom sheet
    if (showLongPressSpeedDialog) {
        SettingsOptionSheet(
            title = "Long-press Speed",
            options = listOf(1.5f, 2.0f, 2.5f, 3.0f),
            selectedOption = longPressSpeed,
            optionLabel = { "${it}x" },
            onOptionSelected = {
                viewModel.updateLongPressSpeed(it)
                showLongPressSpeedDialog = false
            },
            onDismiss = { showLongPressSpeedDialog = false },
            sheetState = sheetState
        )
    }
    
    // Controls Timeout selection bottom sheet
    if (showControlsTimeoutDialog) {
        SettingsOptionSheet(
            title = "Controls Hide Delay",
            options = listOf(2000, 3000, 5000, 10000),
            selectedOption = controlsTimeout,
            optionLabel = { "${it / 1000} seconds" },
            onOptionSelected = {
                viewModel.updateControlsTimeout(it)
                showControlsTimeoutDialog = false
            },
            onDismiss = { showControlsTimeoutDialog = false },
            sheetState = sheetState
        )
    }

    val topBarHeight = 64.dp
    val topInset = androidx.compose.foundation.layout.WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    
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
                    top = topInset + topBarHeight
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
                // Default Player
                SettingsItem(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Default Player",
                    subtitle = if (defaultPlayerType == "MPV") "MPV Player" else "ExoPlayer",
                    onClick = { showPlayerDialog = true }
                )
                
                // Default Orientation
                SettingsItemToggle(
                    icon = Icons.Outlined.ScreenRotation,
                    title = "Default Orientation",
                    subtitle = if (defaultOrientation) "Landscape" else "Portrait",
                    checked = defaultOrientation,
                    onCheckedChange = { viewModel.updateDefaultOrientation(it) }
                )
                
                // Default Playback Speed
                SettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Default Playback Speed",
                    subtitle = "${defaultSpeed}x",
                    onClick = { showSpeedDialog = true }
                )
                
                // Default Aspect Ratio
                SettingsItem(
                    icon = Icons.Outlined.AspectRatio,
                    title = "Default Aspect Ratio",
                    subtitle = defaultAspectRatio,
                    onClick = { showAspectRatioDialog = true }
                )
                
                // Default Decoder
                SettingsItem(
                    icon = Icons.Outlined.Memory,
                    title = "Default Decoder",
                    subtitle = defaultDecoder,
                    onClick = { showDecoderDialog = true }
                )
                
                // Double-tap Seek Duration
                SettingsItem(
                    icon = Icons.Outlined.FastForward,
                    title = "Double-tap Seek",
                    subtitle = "${seekDuration}s",
                    onClick = { showSeekDurationDialog = true }
                )
                
                // Long-press Speed
                SettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Long-press Speed",
                    subtitle = "${longPressSpeed}x",
                    onClick = { showLongPressSpeedDialog = true }
                )
                
                // Controls Timeout
                SettingsItem(
                    icon = Icons.Outlined.Timer,
                    title = "Controls Hide Delay",
                    subtitle = "${controlsTimeout / 1000}s",
                    onClick = { showControlsTimeoutDialog = true }
                )
                
                // Auto-play Next
                SettingsItemToggle(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = "Auto-play Next",
                    subtitle = "Automatically play next video",
                    checked = autoPlayNext,
                    onCheckedChange = { viewModel.updateAutoPlayNext(it) }
                )
                
                // Resume Playback
                SettingsItemToggle(
                    icon = Icons.Outlined.History,
                    title = "Resume Playback",
                    subtitle = "Continue from last position",
                    checked = resumePlayback,
                    onCheckedChange = { viewModel.updateResumePlayback(it) }
                )
                
                // Keep Screen On
                SettingsItemToggle(
                    icon = Icons.Outlined.BrightnessHigh,
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from dimming",
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = BrandAccent.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    BrandAccent.copy(alpha = 0.12f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SettingsItemToggle(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        BrandAccent.copy(alpha = 0.12f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
        } else {
            Spacer(Modifier.width(54.dp)) // Indent to align with items that have icons
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandAccent,
                checkedTrackColor = BrandAccent.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsOptionSheet(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            options.forEach { option ->
                val isSelected = option == selectedOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = isSelected,
                        onClick = { onOptionSelected(option) },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = BrandAccent
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = optionLabel(option),
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

