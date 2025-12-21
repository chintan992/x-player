package com.chintan992.xplayer.data

import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class SubtitleResult(
    val id: String,
    val name: String,
    val language: String,
    val downloadUrl: String
)

@Singleton
class SubtitleRepository @Inject constructor() {

    // Placeholder for API Key provided configuration
    private val fakeResults = listOf(
        SubtitleResult("1", "English Subtitles - TeamRelease", "English", "https://example.com/sub1.srt"),
        SubtitleResult("2", "Spanish Subtitles - Official", "Spanish", "https://example.com/sub2.srt"),
        SubtitleResult("3", "French Subtitles", "French", "https://example.com/sub3.srt")
    )

    fun searchSubtitles(query: String): Flow<List<SubtitleResult>> = flow {
        // Simulate network delay
        delay(1500)
        
        // Mock logic - in production integrate OpenSubtitles REST API
        if (query.isBlank()) {
            emit(emptyList())
        } else {
            // Return fake results for any query for demonstration
            emit(fakeResults)
        }
    }
    
    suspend fun downloadSubtitle(url: String): Uri? {
        delay(1000)
        // In real app, download file to private storage and return File URI
        // Returning null for mock as we can't actually download from example.com
        return null 
    }
}
