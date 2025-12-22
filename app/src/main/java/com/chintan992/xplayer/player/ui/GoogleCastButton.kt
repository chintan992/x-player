package com.chintan992.xplayer.player.ui

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GoogleCastButton(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            val contextWrapper = ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            androidx.mediarouter.app.MediaRouteButton(contextWrapper).apply {
                try {
                    com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(
                        context.applicationContext,
                        this
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        },
        modifier = modifier.size(48.dp)
    )
}
