package com.chintan992.xplayer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chintan992.xplayer.LibraryScreen
import com.chintan992.xplayer.PlaylistManager
import com.chintan992.xplayer.R
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
    
    // Bottom Navigation Items
    val items = listOf(
        BottomNavItem.Folders,
        BottomNavItem.Recent,
        BottomNavItem.Playlist,
        BottomNavItem.Network
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

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            NavigationBar {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Folders.route,
            modifier = Modifier.padding(innerPadding)
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
                        rootNavController.navigate("settings")
                    },
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = sharedTransitionScope
                )
            }
            composable(BottomNavItem.Recent.route) {
                // Placeholder
                TempScreen("Recent")
            }
            composable(BottomNavItem.Playlist.route) {
                // Placeholder
                TempScreen("Playlist")
            }
            composable(BottomNavItem.Network.route) {
                // Placeholder
                NetworkScreen()
            }
        }
    }
}

@Composable
fun TempScreen(name: String) {
     androidx.compose.foundation.layout.Box(
         modifier = Modifier.fillMaxSize(), 
         contentAlignment = androidx.compose.ui.Alignment.Center
     ) {
         Text("TODO: $name Screen")
     }
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
    object Recent : BottomNavItem(
        route = "recent",
        title = "Recent",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
    object Playlist : BottomNavItem(
        route = "playlist",
        title = "Playlist",
        selectedIcon = Icons.Filled.PlaylistPlay,
        unselectedIcon = Icons.Outlined.PlaylistPlay
    )
    object Network : BottomNavItem(
        route = "network",
        title = "Network",
        selectedIcon = Icons.Filled.Wifi,
        unselectedIcon = Icons.Outlined.Wifi
    )
}
