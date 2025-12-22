package com.chintan992.xplayer.wear.presentation

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.ui.PlayerView
import com.chintan992.xplayer.wear.R
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WearMainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var audioManager: android.media.AudioManager
    private var isLocked = false
    private lateinit var lockButton: android.widget.ImageButton
    private lateinit var controlsRoot: android.view.ViewGroup
    private lateinit var vibrator: android.os.Vibrator

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var idleContainer: View
    private lateinit var loadingIndicator: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_main)

        audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator

        playerView = findViewById(R.id.player_view)
        idleContainer = findViewById(R.id.idle_container)
        loadingIndicator = findViewById(R.id.loading_indicator)

        // Configure LoadControl for aggressive buffering
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                3000,  // Min buffer
                10000, // Max buffer
                1000,  // Buffer for playback start
                2000   // Buffer for rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Audio Attributes
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true) // Handle audio focus
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keep Wi-Fi awake
            .build()

        playerView.player = player
        playerView.keepScreenOn = true // Keep screen awake
        
        lockButton = findViewById(R.id.lock_button)
        lockButton.setOnClickListener {
            vibrateTick()
            toggleLock()
        }

        // Lock Logic
        // Use post to ensure view is inflated and we can find sub-views
        playerView.post {
            controlsRoot = playerView.findViewById(R.id.center_controls)
            val rewindButton = playerView.findViewById<View>(R.id.exo_rew)
            val forwardButton = playerView.findViewById<View>(R.id.exo_ffwd)
            val playButton = playerView.findViewById<View>(R.id.exo_play)
            val pauseButton = playerView.findViewById<View>(R.id.exo_pause)

            // Manual wiring for play/pause to ensure they work reliably
            playButton?.setOnClickListener {
                vibrateTick()
                safePlayerCommand { player.play() }
            }
            pauseButton?.setOnClickListener {
                vibrateTick()
                safePlayerCommand { player.pause() }
            }
            
            rewindButton?.setOnClickListener {
                vibrateTick()
                safePlayerCommand { player.seekBack() }
            }
            forwardButton?.setOnClickListener {
                vibrateTick()
                safePlayerCommand { player.seekForward() }
            }
        }
        
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) {
                    loadingIndicator.visibility = View.VISIBLE
                } else {
                    loadingIndicator.visibility = View.GONE
                }
            }
        })
        
        // Request focus for rotary input
        playerView.focusable = View.FOCUSABLE
        playerView.requestFocus()
    }

    private fun safePlayerCommand(action: () -> Unit) {
        try {
            if (player.playbackState != Player.STATE_IDLE) {
                action()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun vibrateTick() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
        } else {
            vibrator.vibrate(50) // Fallback
        }
    }
    
    private fun toggleLock() {
        isLocked = !isLocked
        
        if (isLocked) {
            // Locked State
            lockButton.setImageResource(android.R.drawable.ic_lock_lock)
            lockButton.setColorFilter(android.graphics.Color.RED)
            
            // Hide player controls
            playerView.useController = false
            controlsRoot.visibility = View.GONE
        } else {
            // Unlocked State
            lockButton.setImageResource(android.R.drawable.ic_lock_lock)
            lockButton.setColorFilter(android.graphics.Color.WHITE)
            
            // Show player controls
            playerView.useController = true
            playerView.showController()
            controlsRoot.visibility = View.VISIBLE
        }
    }

    // Rotary Input for Volume
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (isLocked) return super.onKeyDown(keyCode, event)
        
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                vibrateTick()
                audioManager.adjustStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_RAISE,
                    android.media.AudioManager.FLAG_SHOW_UI
                )
                return true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                vibrateTick()
                audioManager.adjustStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_LOWER,
                    android.media.AudioManager.FLAG_SHOW_UI
                )
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    // For Rotary Scroll events (Digital Crown / Bezel)
    override fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (isLocked) return super.onGenericMotionEvent(event)
        
        if (event.action == android.view.MotionEvent.ACTION_SCROLL &&
            androidx.core.view.MotionEventCompat.isFromSource(event, android.view.InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            val delta = -event.getAxisValue(android.view.MotionEvent.AXIS_SCROLL)
            val direction = if (delta > 0) android.media.AudioManager.ADJUST_LOWER else android.media.AudioManager.ADJUST_RAISE
            
            // Only vibrate on scroll if we are actually changing volume (optional throttling could be added)
            vibrateTick() 
            
            audioManager.adjustStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                direction,
                android.media.AudioManager.FLAG_SHOW_UI
            )
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        if (player.isPlaying) {
             // Keep player visible if already playing
             idleContainer.visibility = View.GONE
             playerView.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/play_stream") {
            val data = String(messageEvent.data, StandardCharsets.UTF_8)
            try {
                val json = JSONObject(data)
                val streamUrl = json.getString("url")
                val title = json.optString("title", "")
                
                runOnUiThread {
                    playVideo(streamUrl, title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playVideo(url: String, title: String) {
        idleContainer.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        loadingIndicator.visibility = View.VISIBLE

        // Update title in custom controller
        val titleView = playerView.findViewById<TextView>(R.id.video_title)
        titleView?.text = title

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
}
