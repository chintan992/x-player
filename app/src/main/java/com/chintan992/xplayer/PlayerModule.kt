package com.chintan992.xplayer

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton
import com.chintan992.xplayer.player.abstraction.UniversalPlayer
import com.chintan992.xplayer.player.abstraction.ExoPlayerWrapper
import com.chintan992.xplayer.player.abstraction.MPVPlayerWrapper
import javax.inject.Named

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "library_preferences")

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideHeaderStorage(): HeaderStorage = HeaderStorage()

    @Provides
    @Singleton
    fun provideCustomHeaderInterceptor(headerStorage: HeaderStorage): CustomHeaderInterceptor {
        return CustomHeaderInterceptor(headerStorage)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(customHeaderInterceptor: CustomHeaderInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(customHeaderInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ExoPlayer {
        // OkHttpDataSource for network requests (HTTP/HTTPS URLs)
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        // DefaultDataSource wraps httpDataSourceFactory and adds support for
        // content://, file://, asset://, and other local URI schemes
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Custom LoadControl to start playback faster
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                50000, // maxBufferMs
                500,   // bufferForPlaybackMs (Start playing after 500ms buffered)
                1000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        // Use DefaultRenderersFactory to enable extension renderers (like FFmpeg)
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    dataSourceFactory,
                    com.chintan992.xplayer.extractor.CustomExtractorsFactory()
                )
            )
            .setLoadControl(loadControl)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("EXO")
    fun provideExoPlayerWrapper(
        player: ExoPlayer,
        trackManager: com.chintan992.xplayer.player.logic.TrackManager,
        headerStorage: HeaderStorage
    ): UniversalPlayer {
        return ExoPlayerWrapper(player, trackManager, headerStorage)
    }

    @Provides
    @Singleton
    @javax.inject.Named("MPV")
    fun provideMpvPlayerWrapper(@ApplicationContext context: Context): UniversalPlayer {
        return MPVPlayerWrapper(context)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
