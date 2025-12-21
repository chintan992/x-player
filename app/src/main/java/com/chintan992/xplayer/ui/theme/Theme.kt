package com.chintan992.xplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryDark,
    primaryContainer = SurfaceContainerDark, // Neutral container
    onPrimaryContainer = Color.White,
    secondary = BrandAccent, // Use BrandAccent as Secondary in Dark Mode for subtle pops
    onSecondary = Color.White,
    secondaryContainer = BrandAccentDark,
    onSecondaryContainer = Color(0xFFFFD8EC),
    tertiary = Tertiary,
    onTertiary = Color.Black,
    tertiaryContainer = TertiaryDark,
    onTertiaryContainer = Color(0xFFD8D3F4),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    error = ErrorColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black, // Bold clean black for light mode primary
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5F5F5),
    onPrimaryContainer = Color.Black,
    secondary = BrandAccent,
    onSecondary = Color.White,
    secondaryContainer = BrandAccentLight,
    onSecondaryContainer = Color(0xFF3E002C),
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = Color(0xFF170047),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun XPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Enable dynamic color for Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}