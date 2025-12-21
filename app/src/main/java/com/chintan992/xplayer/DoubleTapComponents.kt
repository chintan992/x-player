package com.chintan992.xplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class DoubleTapSide { LEFT, RIGHT }

@Composable
fun DoubleTapOverlay(
    side: DoubleTapSide,
    onDismiss: () -> Unit
) {
    val composition = remember { Animatable(0f) }
    
    LaunchedEffect(side) {
        composition.snapTo(0f)
        composition.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        onDismiss()
    }

    val alpha = 1f - composition.value
    // Unused scale variable
    // val scale = 1f + composition.value * 1.5f
    // Unused variable
    // val translationX = if (side == DoubleTapSide.LEFT) -50.dp else 50.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .align(if (side == DoubleTapSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .background(
                    Brush.horizontalGradient(
                        colors = if (side == DoubleTapSide.LEFT) 
                            listOf(Color.White.copy(alpha = 0.2f * alpha), Color.Transparent)
                        else 
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.2f * alpha))
                    ),
                    shape = if (side == DoubleTapSide.LEFT) 
                        RoundedCornerShape(topEnd = 200.dp, bottomEnd = 200.dp) 
                    else 
                        RoundedCornerShape(topStart = 200.dp, bottomStart = 200.dp)
                )
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 Icon(
                    imageVector = if (side == DoubleTapSide.LEFT) Icons.Default.FastRewind else Icons.Default.FastForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = alpha),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "10s",
                    color = Color.White.copy(alpha = alpha),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
