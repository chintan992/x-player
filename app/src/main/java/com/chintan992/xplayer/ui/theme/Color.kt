package com.chintan992.xplayer.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Identity
// Toned down "Cinema Pink" for subtle accents (play buttons, selection)
val BrandAccent = Color(0xFFC2185B) // Darker Pink
val BrandAccentLight = Color(0xFFE91E63) // Standard Pink
val BrandAccentDark = Color(0xFF880E4F) // Deep Pink

// Neutral Primary (for clean, content-first look)
val Primary = Color.Black // In light mode, primary UI is often black/dark grey
val PrimaryContainer = Color(0xFFF0F0F0)
val OnPrimaryContainer = Color.Black

val PrimaryDark = Color.White
val PrimaryContainerDark = Color(0xFF202020)
val OnPrimaryContainerDark = Color.White

// Secondary color: Complementary or subtle
val Secondary = BrandAccent
val SecondaryContainer = BrandAccentLight.copy(alpha = 0.1f)
val OnSecondaryContainer = Color(0xFF3E002C)

// Tertiary/Accent color
val Tertiary = Color(0xFF03DAC6)
val TertiaryContainer = Color(0xFFE0F7FA)
val OnTertiaryContainer = Color(0xFF004D40)

// Surface colors for dark theme - Pure Black for Cinema Feel
val SurfaceDark = Color(0xFF000000)
val SurfaceContainerDark = Color(0xFF161616) // Slightly lighter for cards/lists
val SurfaceContainerHighDark = Color(0xFF252525) // For modals/dialogs
val SurfaceVariantDark = Color(0xFF333333)

// Surface colors for light theme
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFF6F6F6)
val SurfaceContainerHighLight = Color(0xFFECECEC)
val SurfaceVariantLight = Color(0xFFE0E0E0)

// Background colors
val BackgroundDark = Color(0xFF000000)
val BackgroundLight = Color(0xFFFFFFFF)

// On colors
val OnPrimaryDark = Color.Black
val OnPrimaryLight = Color.White
val OnSecondaryDark = Color.White
val OnSurfaceDark = Color(0xFFFFFFFF) // Pure White for max contrast
val OnSurfaceLight = Color(0xFF1C1B1F)
val OnSurfaceVariantDark = Color(0xFFE0E0E0) // Lighter Gray
val OnSurfaceVariantLight = Color(0xFF49454F)

// Status colors
val ErrorColor = Color(0xFFB3261E)
val SuccessColor = Color(0xFF4CAF50)

// Cinema Theme (Strict Dark Mode for Player)
val CinemaBackground = Color.Black

// UI Refinements for Card/List Design
val SurfaceContainerLowDark = Color(0xFF1D1B20) // Darker card background
val OutlineVariantDark = Color(0xFF49454F) // Border color for cards
val SurfaceVariantDarkHigh = Color(0xFF2C2C2C) // Slightly lighter for component backgrounds
val CinemaOnBackground = Color.White
val CinemaSurface = Color(0xFF121212)
val CinemaOnSurface = Color(0xFFFFFFFF) // Pure White
val CinemaAccent = BrandAccentLight