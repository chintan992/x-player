package com.chintan992.xplayer.data.local

import android.net.Uri
import com.chintan992.xplayer.VideoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import javax.inject.Inject

class FileScanner @Inject constructor(
    private val subtitleFinder: SubtitleFinder
) {

    suspend fun scanDirectoryForVideos(dir: File, recursive: Boolean): List<VideoItem> = kotlinx.coroutines.coroutineScope {
        val files = dir.listFiles() ?: return@coroutineScope emptyList()
        val deferredVideos = mutableListOf<kotlinx.coroutines.Deferred<List<VideoItem>>>()
        val directVideos = mutableListOf<kotlinx.coroutines.Deferred<VideoItem?>>()

        for (file in files) {
            if (file.isDirectory) {
                if (recursive) {
                    deferredVideos.add(async { scanDirectoryForVideos(file, true) })
                }
            } else {
                if (isValidVideoFile(file.name)) {
                     directVideos.add(async(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val uri = Uri.fromFile(file)
                            val name = file.name
                            val parent = file.parentFile
                            val folderName = parent?.name ?: "Unknown"
                            val folderPath = parent?.absolutePath ?: ""
                            val size = file.length()
                            val modified = file.lastModified() / 1000
    
                            val duration = getDuration(file)
    
                            VideoItem(
                                id = file.hashCode().toLong(), // Synthetic ID
                                uri = uri,
                                name = name,
                                duration = duration,
                                size = size,
                                dateModified = modified,
                                folderPath = folderPath,
                                folderName = folderName,
                                subtitleUri = subtitleFinder.findSubtitleForVideo(file.absolutePath)
                            )
                        } catch (e: Exception) {
                            null
                        }
                    })
                }
            }
        }
        
        val allVideos = mutableListOf<VideoItem>()
        // Await recursive results
        deferredVideos.awaitAll().forEach { allVideos.addAll(it) }
        // Await direct file results
        directVideos.awaitAll().forEach { it?.let { video -> allVideos.add(video) } }
        
        allVideos
    }
    
    private fun isValidVideoFile(name: String): Boolean {
        val extensions = arrayOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".3gp")
        return extensions.any { name.endsWith(it, ignoreCase = true) }
    }

    private fun getDuration(file: File): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
