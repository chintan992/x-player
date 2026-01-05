package com.chintan992.xplayer.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chintan992.xplayer.ui.theme.BrandAccent
import com.chintan992.xplayer.ui.theme.BrandAccentDark
import com.chintan992.xplayer.ui.theme.BrandAccentLight
import com.chintan992.xplayer.ui.theme.OutlineVariantDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen() {
    var streamUrl by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Network", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stream Link Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Stream Link",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OutlineVariantDark, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = streamUrl,
                                onValueChange = { streamUrl = it },
                                placeholder = { Text("Video URL") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = OutlineVariantDark,
                                    focusedBorderColor = BrandAccent,
                                    cursorColor = BrandAccent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    clipboardManager.getText()?.text?.let {
                                        streamUrl = it
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Paste")
                            }
                        }
                        
                        // Play button with gradient and press animation
                        var isPlayPressed by remember { mutableStateOf(false) }
                        val playButtonScale by animateFloatAsState(
                            targetValue = if (isPlayPressed) 0.96f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh
                            ),
                            label = "playButtonScale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .scale(playButtonScale)
                                .clip(RoundedCornerShape(25.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(BrandAccent, BrandAccentLight)
                                    )
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPlayPressed = true
                                            tryAwaitRelease()
                                            isPlayPressed = false
                                        },
                                        onTap = {
                                            // TODO: Play Logic
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Play",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            // Local Network Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Local Network",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes remaining space
                        .border(1.dp, OutlineVariantDark, RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Icon Placeholder
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
//                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp)),
                            ) {
                                // Using a generic share icon as placeholder for the network diagram in mockup
                                Icon(
                                    Icons.Outlined.Share, 
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)
                                )
                                Icon(
                                    Icons.Outlined.QuestionMark,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                "No connections added. Tap 'Add Connection' to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        
                        // Floating Button at Bottom Right
                        Button(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandAccent.copy(alpha = 0.2f),
                                contentColor = BrandAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Connection")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
