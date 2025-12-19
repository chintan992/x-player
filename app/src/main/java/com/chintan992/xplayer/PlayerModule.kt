package com.chintan992.xplayer

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(CustomHeaderInterceptor())
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
        
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
    }
}
