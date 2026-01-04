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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
// import com.chintan992.xplayer.data.PrivacyPolicyRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

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

//    @Inject
//    lateinit var privacyPolicyRepository: PrivacyPolicyRepository

    private val pipPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePipActions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkIntent = intent

        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                val permissionLauncher = registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        // Permission granted
                    } else {
                        // Permission denied
                    }
                }
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        
        // Register PiP broadcast receiver
        registerPipReceiver()
        
        // Start PlaybackService to keep MediaSession active
        try {
            val intent = Intent(this, PlaybackService::class.java)
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize CastContext
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        setContent {
            XPlayerTheme {
                // val isPolicyAccepted by privacyPolicyRepository.isPolicyAccepted.collectAsState(initial = null)
                
                // when (isPolicyAccepted) {
                //     true -> {
                        // Policy Accepted: Show Main Content
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController, 
                            player = player,
                            deepLinkIntent = deepLinkIntent,
                            resolveVideoTitle = ::resolveVideoTitle,
                            onEnterPip = { enterPipMode() }
                        )
                //     }
                //     else -> {
                //          // Fallback to content
                //     }
                // }
            }
        }
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
                             player.seekTo((player.currentPosition - PlayerConfig.SEEK_INCREMENT_MS).coerceAtLeast(0))
                         }
                         CONTROL_FORWARD -> {
                             player.seekTo((player.currentPosition + PlayerConfig.SEEK_INCREMENT_MS).coerceAtMost(player.duration))
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
        val videoSize = player.videoSize
        val aspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
            val width = videoSize.width
            val height = videoSize.height
            val ratio = width.toFloat() / height
            
            if (ratio in PlayerConfig.PIP_ASPECT_RATIO_MIN..PlayerConfig.PIP_ASPECT_RATIO_MAX) {
                Rational(width, height)
            } else {
                Rational(PlayerConfig.PIP_DEFAULT_ASPECT_RATIO_NUMERATOR, PlayerConfig.PIP_DEFAULT_ASPECT_RATIO_DENOMINATOR)
            }
        } else {
            Rational(PlayerConfig.PIP_DEFAULT_ASPECT_RATIO_NUMERATOR, PlayerConfig.PIP_DEFAULT_ASPECT_RATIO_DENOMINATOR)
        }

        val actions = mutableListOf<RemoteAction>()
        val seekSeconds = PlayerConfig.SEEK_INCREMENT_MS / 1000
        
        // Rewind
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                "Rewind",
                "Rewind $seekSeconds seconds",
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
        
        // Forward
        actions.add(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                "Forward",
                "Forward $seekSeconds seconds",
                createPipPendingIntent(CONTROL_FORWARD, 2)
            )
        )
        
        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode) {
                // Listen for play state changes to update PiP controls
                player.addListener(pipPlayerListener)
                updatePipActions()
            } else {
                // Remove listener when exiting PiP
                player.removeListener(pipPlayerListener)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            // Do NOT release singleton player, just pause/stop
            if (player.isPlaying) player.pause()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player.isPlaying) {
            enterPipMode()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkIntent = intent
    }

    private var deepLinkIntent by mutableStateOf<Intent?>(null)

    private fun resolveVideoTitle(uri: Uri): String {
        var title = "External Video"
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        title = it.getString(displayNameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return title
    }

    // ... existing onDestroy ...
    override fun onDestroy() {
        super.onDestroy()
        pipReceiver?.let { unregisterReceiver(it) }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    player: ExoPlayer,
    deepLinkIntent: Intent?,
    resolveVideoTitle: (Uri) -> String,
    onEnterPip: () -> Unit = {}
) {
    // Store current video info for player screen
    var currentVideoTitle by remember { mutableStateOf("") }
    var currentVideoUri by remember { mutableStateOf<String?>(null) }
    var currentVideoId by remember { mutableStateOf<String?>(null) }
    var currentSubtitleUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Handle Deep Link
    androidx.compose.runtime.LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.let { intent ->
            if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                val uri = intent.data!!
                currentVideoUri = uri.toString()
                currentVideoTitle = resolveVideoTitle(uri)
                currentVideoId = uri.toString() // Use URI as ID for external videos
                currentSubtitleUri = null // No subtitle support for external yet
                
                // Clear playlist for external video
                com.chintan992.xplayer.PlaylistManager.clearPlaylist()
                
                navController.navigate("player") {
                     launchSingleTop = true
                }
            }
        }
    }

    androidx.compose.animation.SharedTransitionLayout {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                 com.chintan992.xplayer.ui.MainScreen(
                     rootNavController = navController,
                     player = player,
                     sharedTransitionScope = this@SharedTransitionLayout,
                     animatedVisibilityScope = this
                 )
            }
            
            composable(
                route = "player_route/{videoUri}?title={title}&id={id}",
                arguments = listOf(
                    androidx.navigation.navArgument("videoUri") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("title") { 
                        type = androidx.navigation.NavType.StringType 
                        defaultValue = ""
                    },
                    androidx.navigation.navArgument("id") { 
                        type = androidx.navigation.NavType.StringType 
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val videoUri = backStackEntry.arguments?.getString("videoUri")
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                
                // Force Cinema Theme (Dark Mode, No Dynamic Color) for Player
                XPlayerTheme(darkTheme = true, dynamicColor = false) {
                    VideoPlayerScreen(
                        player = player,
                        videoTitle = title,
                        videoUri = videoUri,
                        videoId = id,
                        subtitleUri = null, // TODO: Pass subtitle URI if needed
                        onBackPressed = {
                            player.pause()
                            navController.popBackStack()
                        },
                        onEnterPip = onEnterPip,
                        animatedVisibilityScope = this,
                        sharedTransitionScope = this@SharedTransitionLayout
                    )
                }
            }
            
            // Legacy/DeepLink player route (kept for compatibility/deeplinks if needed directly)
            composable("player") {
                XPlayerTheme(darkTheme = true, dynamicColor = false) {
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
                        onEnterPip = onEnterPip,
                        animatedVisibilityScope = this,
                        sharedTransitionScope = this@SharedTransitionLayout
                    )
                }
            }
            
            composable("settings") {
                com.chintan992.xplayer.ui.SettingsScreen(navController = navController)
            }
        }
    }
}
