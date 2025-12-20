package com.chintan992.xplayer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.chintan992.xplayer.ui.theme.XPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var player: ExoPlayer

    private var pipReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_PIP_CONTROL = "com.chintan992.xplayer.PIP_CONTROL"
        const val EXTRA_CONTROL_TYPE = "control_type"
        const val CONTROL_PLAY_PAUSE = 1
        const val CONTROL_REWIND = 2
        const val CONTROL_FORWARD = 3
    }

    @Inject
    lateinit var headerStorage: HeaderStorage

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verify Header Injection
        verifyHeaderInjection()
        
        // Register PiP broadcast receiver
        registerPipReceiver()
        
        setContent {
            XPlayerTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController, 
                    player = player,
                    onEnterPip = { enterPipMode() }
                )
            }
        }
    }

    private fun verifyHeaderInjection() {
        val host = "httpbin.org"
        val headers = mapOf("X-Test-Header" to "VerificationValue")
        headerStorage.addHeaders(host, headers)

        // Run network request in background
        Thread {
            try {
                val request = okhttp3.Request.Builder()
                    .url("https://httpbin.org/get")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("VerificationValue")) {
                        android.util.Log.d("HeaderVerification", "SUCCESS: Custom header injected and received.")
                    } else {
                        android.util.Log.e("HeaderVerification", "FAILURE: Custom header NOT found in response.")
                    }
                } else {
                    android.util.Log.e("HeaderVerification", "FAILURE: Request failed with code ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                android.util.Log.e("HeaderVerification", "ERROR: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    private fun registerPipReceiver() {
        pipReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_PIP_CONTROL) {
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_PLAY_PAUSE -> {
                            if (player.isPlaying) player.pause() else player.play()
                            updatePipActions()
                        }
                        CONTROL_REWIND -> {
                            player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                        }
                        CONTROL_FORWARD -> {
                            player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(ACTION_PIP_CONTROL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipReceiver, filter)
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = buildPipParams()
            enterPictureInPictureMode(params)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val actions = mutableListOf<RemoteAction>()
        
        // Rewind 10s
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                "Rewind",
                "Rewind 10 seconds",
                createPipPendingIntent(CONTROL_REWIND, 0)
            )
        )
        
        // Play/Pause
        val playPauseIcon = if (player.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (player.isPlaying) "Pause" else "Play"
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, playPauseIcon),
                playPauseTitle,
                playPauseTitle,
                createPipPendingIntent(CONTROL_PLAY_PAUSE, 1)
            )
        )
        
        // Forward 10s
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                "Forward",
                "Forward 10 seconds",
                createPipPendingIntent(CONTROL_FORWARD, 2)
            )
        )
        
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipPendingIntent(controlType: Int, requestCode: Int): PendingIntent {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            putExtra(EXTRA_CONTROL_TYPE, controlType)
            setPackage(packageName)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, 
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            // Listen for play state changes to update PiP controls
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePipActions()
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            player.release()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player.isPlaying) {
            enterPipMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pipReceiver?.let { unregisterReceiver(it) }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    player: ExoPlayer,
    onEnterPip: () -> Unit = {}
) {
    // Store current video info for player screen
    var currentVideoTitle by remember { mutableStateOf("") }
    var currentVideoUri by remember { mutableStateOf<String?>(null) }
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var currentSubtitleUri by remember { mutableStateOf<android.net.Uri?>(null) }

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(onVideoClick = { videoItem ->
                // Store video info for player screen
                currentVideoTitle = videoItem.name
                currentVideoUri = videoItem.uri.toString()
                currentVideoId = videoItem.id.toString()
                currentSubtitleUri = videoItem.subtitleUri
                
                // Set current index in PlaylistManager for seamless navigation reference
                // (Though PlayerViewModel will recalculate, it's good to sync)
                val playlist = PlaylistManager.currentPlaylist
                val index = playlist.indexOfFirst { it.id == videoItem.id }
                if (index != -1) {
                    PlaylistManager.currentVideoIndex = index
                }

                navController.navigate("player")
            })
        }
        composable("player") {
            VideoPlayerScreen(
                player = player,
                videoTitle = currentVideoTitle,
                videoUri = currentVideoUri,
                videoId = currentVideoId,
                subtitleUri = currentSubtitleUri,
                onBackPressed = {
                    player.pause()
                    navController.popBackStack()
                },
                onEnterPip = onEnterPip
            )
        }
    }
}
