package com.chintan992.xplayer.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chintan992.xplayer.LibraryScreen
import com.chintan992.xplayer.PlaylistManager
import com.chintan992.xplayer.ui.theme.BrandAccent

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController,
    player: ExoPlayer,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val navController = rememberNavController()
    
    // Bottom Navigation Items: Folders, Network, Settings
    val items = listOf(
        BottomNavItem.Folders,
        BottomNavItem.Network,
        BottomNavItem.Settings
    )
    
    // Store video interaction functions to pass down
    fun onVideoClick(
        videoUri: String, 
        videoId: String, 
        videoName: String, 
        subtitleUri: Uri?
    ) {
         // We navigate using the ROOT controller because Player is full screen Covers bottom bar
         val encodedUri = java.net.URLEncoder.encode(videoUri, java.nio.charset.StandardCharsets.UTF_8.toString())
         val encodedTitle = java.net.URLEncoder.encode(videoName, java.nio.charset.StandardCharsets.UTF_8.toString())
         val encodedId = java.net.URLEncoder.encode(videoId, java.nio.charset.StandardCharsets.UTF_8.toString())

         rootNavController.navigate("player_route/$encodedUri?title=$encodedTitle&id=$encodedId") {
             launchSingleTop = true
         }
    }

    // Calculate Bottom Bar Height + System Navigation Bar Height
    // Standard NavigationBar height is 80.dp
    // We pass this padding to the screens so they can pad their lists ABOVE the bottom bar
    // while the content (backgrounds etc) can extend BEHIND it.
    val navBarHeight = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeight = 80.dp
    val totalBottomPadding = navBarHeight + bottomBarHeight
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = totalBottomPadding)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Folders.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Folders.route) {
                LibraryScreen(
                    onVideoClick = { videoItem -> 
                         // Logic from MainActivity moved/adapted
                         val playlist = PlaylistManager.currentPlaylist
                         val index = playlist.indexOfFirst { it.id == videoItem.id }
                         if (index != -1) {
                            PlaylistManager.currentVideoIndex = index
                         }
                         
                         // Navigate via wrapper callback
                         onVideoClick(
                             videoItem.uri.toString(), 
                             videoItem.id.toString(), 
                             videoItem.name, 
                             videoItem.subtitleUri
                         )
                    },
                    onSettingsClick = {
                        // Navigate to settings tab instead of separate route
                        navController.navigate(BottomNavItem.Settings.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope,
                    contentPadding = contentPadding
                )
            }
            composable(BottomNavItem.Network.route) {
                NetworkScreen(contentPadding = contentPadding)
            }
            composable(BottomNavItem.Settings.route) {
                // SettingsScreen embedded in bottom nav - pass internal navController
                SettingsScreenEmbedded(contentPadding = contentPadding)
            }
        }
        
        // BOTTOM NAVIGATION (Overlay)
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Only show bottom bar for main tabs
        val showBottomBar = items.any { it.route == currentRoute }
        
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Ensure it's drawn on top
                        shadowElevation = 0f 
                    }
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 0.dp
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        
                        // Animate icon scale with spring physics
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "navIconScale"
                        )
                        
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                ) 
                            },
                            label = { Text(text = item.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandAccent,
                                selectedTextColor = BrandAccent,
                                indicatorColor = BrandAccent.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Embedded version of SettingsScreen without the back button (used in bottom nav)
 */
@Composable
fun SettingsScreenEmbedded(
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    SettingsScreen(navController = null, contentPadding = contentPadding)
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Folders : BottomNavItem(
        route = "folders",
        title = "Folders",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )
    object Network : BottomNavItem(
        route = "network",
        title = "Network",
        selectedIcon = Icons.Filled.Wifi,
        unselectedIcon = Icons.Outlined.Wifi
    )
    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
