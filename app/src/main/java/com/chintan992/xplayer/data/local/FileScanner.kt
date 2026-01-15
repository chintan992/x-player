package com.chintan992.xplayer.data.local

import android.net.Uri
import com.chintan992.xplayer.VideoItem
import java.io.File
import javax.inject.Inject

class FileScanner @Inject constructor(
    private val subtitleFinder: SubtitleFinder
) {

    fun scanDirectoryForVideos(dir: File, recursive: Boolean): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val files = dir.listFiles() ?: return emptyList()
        
        for (file in files) {
            if (file.isDirectory) {
                if (recursive) {
                    videos.addAll(scanDirectoryForVideos(file, true))
                }
            } else {
                if (isValidVideoFile(file.name)) {
                    val uri = Uri.fromFile(file)
                    val name = file.name
                    val parent = file.parentFile
                    val folderName = parent?.name ?: "Unknown"
                    val folderPath = parent?.absolutePath ?: ""
                    val size = file.length()
                    val modified = file.lastModified() / 1000
                    
                    val duration = getDuration(file)
                    
                    videos.add(VideoItem(
                        id = file.hashCode().toLong(), // Synthetic ID
                        uri = uri,
                        name = name,
                        duration = duration,
                        size = size,
                        dateModified = modified,
                        folderPath = folderPath,
                        folderName = folderName,
                        subtitleUri = subtitleFinder.findSubtitleForVideo(file.absolutePath)
                    ))
                }
            }
        }
        return videos
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
