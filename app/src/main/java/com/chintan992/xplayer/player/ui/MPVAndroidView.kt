package com.chintan992.xplayer.player.ui

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import `is`.xyz.mpv.MPVLib

class MPVAndroidView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    var player: com.chintan992.xplayer.player.abstraction.MPVPlayerWrapper? = null
        set(value) {
            field = value
            if (holder.surface != null && holder.surface.isValid) {
                value?.attachSurface(holder.surface)
            }
        }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        player?.attachSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Critical: MPV needs to know the surface dimensions to render video properly
        // Without this call, MPV shows black screen while audio plays
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        player?.detachSurface()
    }
}
