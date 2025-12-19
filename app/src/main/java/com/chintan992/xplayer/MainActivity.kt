package com.chintan992.xplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.chintan992.xplayer.ui.theme.XPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XPlayerTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController, player = player)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            player.release()
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    player: ExoPlayer
) {
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(onVideoClick = { videoItem ->
                // Use MediaItem.fromUri() with content:// URI for local files
                val mediaItem = MediaItem.fromUri(videoItem.uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                navController.navigate("player")
            })
        }
        composable("player") {
            VideoPlayerScreen(player = player)
        }
    }
}
