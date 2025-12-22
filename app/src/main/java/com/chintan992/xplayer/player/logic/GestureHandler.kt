package com.chintan992.xplayer.player.logic

import androidx.media3.exoplayer.ExoPlayer

import javax.inject.Inject

class GestureHandler @Inject constructor() {

    fun calculateNewLevel(currentLevel: Float, delta: Float): Float {
        return (currentLevel + delta).coerceIn(0f, 1f)
    }

    fun applyVolume(player: ExoPlayer?, volume: Float) {
        player?.volume = volume
    }
}
