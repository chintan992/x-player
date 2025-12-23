package com.chintan992.xplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chintan992.xplayer.R

@Composable
fun PrivacyPolicyDialog(
    onAccept: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Full screen
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.privacy_policy_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val scrollState = rememberScrollState()
                
                // Track if user has scrolled to bottom
                var hasScrolledToBottom by remember { mutableStateOf(false) }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = stringResource(R.string.privacy_policy_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    SectionTitle(stringResource(R.string.privacy_policy_permissions_title))
                    BulletPoint(stringResource(R.string.privacy_policy_storage_permission))
                    BulletPoint(stringResource(R.string.privacy_policy_network_permission))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SectionTitle(stringResource(R.string.privacy_policy_data_collection_title))
                    BulletPoint(stringResource(R.string.privacy_policy_analytics))

                    // Spacer to ensure scrolling is required on smaller screens if content is long
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Detect when scrolled to bottom
                    LaunchedEffect(scrollState.value, scrollState.maxValue) {
                         if (!hasScrolledToBottom && scrollState.value >= scrollState.maxValue - 50) { // Tolerance
                             hasScrolledToBottom = true
                         }
                    }
                    // For short content that doesn't scroll, detect immediately
                    LaunchedEffect(scrollState.maxValue) {
                        if (scrollState.maxValue == 0) {
                            hasScrolledToBottom = true
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!hasScrolledToBottom) {
                    Text(
                        text = stringResource(R.string.privacy_policy_scroll_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = onAccept,
                    enabled = hasScrolledToBottom,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.privacy_policy_accept_btn))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // Bullet
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 12.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
        )
        // Text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}
